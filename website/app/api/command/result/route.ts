import { NextResponse } from 'next/server';
import { adminDb, adminAuth } from '@/lib/firebaseAdmin';

export async function POST(req: Request) {
  try {
    if (!adminAuth || !adminDb) {
      return NextResponse.json({ error: 'Firebase Admin environment not configured' }, { status: 500 });
    }

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
      return NextResponse.json({ error: 'Missing result data' }, { status: 400 });
    }

    const fullCommandName = command || 'unknown';
    const baseCommandName = fullCommandName.split(' ')[0].toLowerCase();

    let payload = result;
    if (typeof result === 'string') {
      try {
        payload = JSON.parse(result);
      } catch (e) {
        payload = { type: 'text', content: result };
      }
    }

    const timestamp = new Date().toISOString();

    // 1. REPLACE latest result doc per command (0 history buildup to save Firestore quota)
    const commandDocRef = adminDb.collection('users').doc(userId).collection('command_history').doc(baseCommandName);
    await commandDocRef.set({
      command: fullCommandName,
      payload: payload,
      timestamp: timestamp
    });

    // 2. Special case for LOCATE command: store history HARD CAPPED TO 5 ENTRIES MAX
    if (baseCommandName === 'locate') {
      const locRef = adminDb.collection('users').doc(userId).collection('location_history');
      await locRef.add({
        command: fullCommandName,
        payload: payload,
        timestamp: timestamp
      });

      const locSnap = await locRef.orderBy('timestamp', 'desc').get();
      if (locSnap.docs.length > 5) {
        const docsToDelete = locSnap.docs.slice(5);
        const batch = adminDb.batch();
        docsToDelete.forEach((d: any) => batch.delete(d.ref));
        await batch.commit();
      }
    }

    return NextResponse.json({ success: true });
  } catch (error: any) {
    console.error('Error saving command result:', error);
    return NextResponse.json({ error: error.message || 'Internal server error' }, { status: 500 });
  }
}
