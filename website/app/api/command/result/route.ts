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
    const { result, command, encrypted } = body;

    if (!result) {
      return NextResponse.json({ error: 'Missing result data' }, { status: 400 });
    }

    const fullCommandName = command || 'unknown';
    const baseCommandName = fullCommandName.split(' ')[0].toLowerCase();

    let payload = result;
    if (encrypted) {
      payload = { type: 'encrypted', content: result };
    } else if (typeof result === 'string') {
      try {
        payload = JSON.parse(result);
      } catch (e) {
        payload = { type: 'text', content: result };
      }
    }

    const timestamp = new Date().toISOString();
    const commandDocRef = adminDb.collection('users').doc(userId).collection('command_history').doc(baseCommandName);

    if (baseCommandName === 'locate') {
      // For locate command: store array of last 5 entries inside users/{userId}/command_history/locate doc
      const existingDoc = await commandDocRef.get();
      let existingHistory: any[] = [];
      if (existingDoc.exists && Array.isArray(existingDoc.data()?.history)) {
        existingHistory = existingDoc.data()?.history;
      }

      const newEntry = {
        id: `loc_${Date.now()}`,
        command: fullCommandName,
        payload: payload,
        timestamp: timestamp
      };

      const updatedHistory = [newEntry, ...existingHistory].slice(0, 5);

      await commandDocRef.set({
        command: fullCommandName,
        payload: payload,
        timestamp: timestamp,
        history: updatedHistory
      });
    } else {
      // For all other commands: REPLACE latest result doc (0 history buildup)
      await commandDocRef.set({
        command: fullCommandName,
        payload: payload,
        timestamp: timestamp
      });
    }

    return NextResponse.json({ success: true });
  } catch (error: any) {
    console.error('Error saving command result:', error);
    return NextResponse.json({ error: error.message || 'Internal server error' }, { status: 500 });
  }
}
