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

    // Save command results into results subcollection
    const isProgressMessage = result.includes('GPS location search initiated') || result.includes('will follow');
    const existingLocateDoc = await adminDb.collection('users').doc(userId).collection('results').doc(commandName).get();
    const hasExistingResult = existingLocateDoc.exists && existingLocateDoc.data()?.result?.includes('maps.google.com');

    if (!isProgressMessage || !hasExistingResult) {
      await adminDb.collection('users').doc(userId).collection('results').doc(commandName).set({
        result: result,
        timestamp: new Date().toISOString()
      }, { merge: true });
    }

    // Save location tracking to locations subcollection (up to 5 items)
    const isLocationResult = commandName === 'autoloc' || commandName === 'locate' || result.includes('maps.google.com') || result.includes('Lat:') || result.includes('lat:');
    if (isLocationResult) {
      let lat = 0;
      let lon = 0;
      let mapsUrl = '';
      let accuracy = '';
      let altitude = '';
      let bearing = '';
      let speed = '';
      let provider = 'GPS';
      let battery = '';

      // Pattern 1: Google Maps URL (supports query=, q=, ll=)
      const mapsMatch = result.match(/https?:\/\/[^\s]*[\?&](?:q|ll|query)=(-?\d+(?:\.\d+)?),(-?\d+(?:\.\d+)?)/i);
      if (mapsMatch) {
        lat = parseFloat(mapsMatch[1]);
        lon = parseFloat(mapsMatch[2]);
        mapsUrl = mapsMatch[0];
      }

      // Pattern 2: Lat: 12.345 and Lon: 67.890
      if (!lat || !lon) {
        const latMatch = result.match(/Lat(?:itude)?:\s*(-?\d+(?:\.\d+)?)/i);
        const lonMatch = result.match(/Lon(?:gitude)?:\s*(-?\d+(?:\.\d+)?)/i);
        if (latMatch && lonMatch) {
          lat = parseFloat(latMatch[1]);
          lon = parseFloat(lonMatch[2]);
        }
      }

      // Pattern 3: Direct coordinate pair "12.345, 67.890"
      if (!lat || !lon) {
        const pairMatch = result.match(/(-?\d{1,2}(?:\.\d+)?)\s*,\s*(-?\d{1,3}(?:\.\d+)?)/);
        if (pairMatch) {
          lat = parseFloat(pairMatch[1]);
          lon = parseFloat(pairMatch[2]);
        }
      }

      const accMatch = result.match(/Accuracy:\s*([\d\.]+\s*m)/i);
      if (accMatch) accuracy = accMatch[1];

      const altMatch = result.match(/Altitude:\s*([\d\.]+\s*m)/i);
      if (altMatch) altitude = altMatch[1];

      const bearMatch = result.match(/Bearing:\s*([\d\.]+)/i);
      if (bearMatch) bearing = `${bearMatch[1]}°`;

      const speedMatch = result.match(/Speed:\s*([^\n]+)/i);
      if (speedMatch) speed = speedMatch[1];

      const provMatch = result.match(/^([A-Za-z0-9_]+):/);
      if (provMatch && provMatch[1] !== 'Lat' && provMatch[1] !== 'Lon' && provMatch[1] !== 'Time' && provMatch[1] !== 'AutoLoc') {
        provider = provMatch[1];
      }

      const battMatch = result.match(/Battery:\s*(\d+\s*%)/i);
      if (battMatch) battery = battMatch[1];

      // Save to locations subcollection whenever valid coordinates exist or for autoloc/locate
      if ((lat !== 0 && lon !== 0) || commandName === 'autoloc' || commandName === 'locate') {
        const locRef = adminDb.collection('users').doc(userId).collection('locations');
        await locRef.add({
          command: commandName,
          sourceType: commandName === 'autoloc' ? 'Auto-Location Background (autoloc)' : 'Manual Request (locate)',
          raw: result,
          timestamp: new Date().toISOString(),
          mapsUrl: mapsUrl || (lat && lon ? `https://maps.google.com/maps?q=${lat},${lon}` : ''),
          lat: lat,
          lon: lon,
          accuracy: accuracy || 'N/A',
          altitude: altitude || 'N/A',
          bearing: bearing || 'N/A',
          speed: speed || 'N/A',
          provider: provider || 'GPS',
          battery: battery || 'N/A'
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
    }

    return NextResponse.json({ success: true });
  } catch (error: any) {
    console.error('Error saving command result:', error);
    return NextResponse.json({ error: error.message || 'Internal server error' }, { status: 500 });
  }
}
