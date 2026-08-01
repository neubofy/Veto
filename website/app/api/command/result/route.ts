import { NextResponse } from 'next/server';
import { adminDb, adminAuth } from '@/lib/firebaseAdmin';

export async function POST(req: Request) {
  try {
    const authHeader = req.headers.get('Authorization');
    if (!authHeader?.startsWith('Bearer ')) {
      return NextResponse.json({ error: 'Unauthorized' }, { status: 401 });
    }

    const token = authHeader.split('Bearer ')[1];
    let decodedToken;
    try {
      decodedToken = await adminAuth.verifyIdToken(token);
    } catch (e) {
      return NextResponse.json({ error: 'Invalid authentication token' }, { status: 401 });
    }

    const userId = decodedToken.uid;
    const body = await req.json();
    const { result, command } = body;

    if (!result) {
      return NextResponse.json({ error: 'Missing result string' }, { status: 400 });
    }

    const commandName = command || 'unknown';

    // Save the command result into a command-specific document
    await adminDb.collection('users').doc(userId).collection('results').doc(commandName).set({
      result: result,
      timestamp: new Date().toISOString()
    }, { merge: true });

    // If result contains location info, save to locations subcollection and prune to 5 max
    if (commandName === 'locate' || commandName === 'autoloc' || result.includes('http://maps.google.com/maps?q=') || result.includes('Lat:')) {
      let lat = 0;
      let lon = 0;
      let mapsUrl = '';
      let accuracy = '';
      let battery = '';

      const mapsMatch = result.match(/https?:\/\/maps\.google\.com\/maps\?q=(-?\d+\.\d+),(-?\d+\.\d+)/);
      if (mapsMatch) {
        lat = parseFloat(mapsMatch[1]);
        lon = parseFloat(mapsMatch[2]);
        mapsUrl = mapsMatch[0];
      }

      const accMatch = result.match(/Accuracy:\s*([\d\.]+\s*m)/i);
      if (accMatch) accuracy = accMatch[1];

      const battMatch = result.match(/Battery:\s*(\d+%)/i);
      if (battMatch) battery = battMatch[1];

      const locRef = adminDb.collection('users').doc(userId).collection('locations');
      await locRef.add({
        raw: result,
        timestamp: new Date().toISOString(),
        mapsUrl: mapsUrl || (lat && lon ? `https://maps.google.com/maps?q=${lat},${lon}` : ''),
        lat,
        lon,
        accuracy,
        battery
      });

      // Keep only 5 most recent locations in Firestore
      const locsSnap = await locRef.orderBy('timestamp', 'desc').get();
      if (locsSnap.docs.length > 5) {
        const docsToDelete = locsSnap.docs.slice(5);
        const batch = adminDb.batch();
        docsToDelete.forEach((doc: any) => batch.delete(doc.ref));
        await batch.commit();
      }
    }

    return NextResponse.json({ success: true });
  } catch (error: any) {
    console.error('Error saving command result:', error);
    return NextResponse.json({ error: error.message || 'Internal server error' }, { status: 500 });
  }
}
