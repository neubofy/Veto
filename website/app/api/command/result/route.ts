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
      return NextResponse.json({ error: 'Missing result data' }, { status: 400 });
    }

    const commandName = command || 'unknown';



    // Save ALL results into a unified command_history collection
    const historyRef = adminDb.collection('users').doc(userId).collection('command_history');
    
    // Check if result is already a JSON object (new app version) or a string (old app version)
    let payload = result;
    if (typeof result === 'string') {
        try {
            payload = JSON.parse(result);
        } catch(e) {
            payload = { type: 'text', content: result };
        }
    }

    await historyRef.add({
      command: commandName,
      payload: payload,
      timestamp: new Date().toISOString()
    });

    // We can limit history to last 50 items to save space
    const historySnap = await historyRef.orderBy('timestamp', 'desc').get();
    if (historySnap.docs.length > 50) {
      const docsToDelete = historySnap.docs.slice(50);
      const batch = adminDb.batch();
      docsToDelete.forEach((doc: any) => batch.delete(doc.ref));
      await batch.commit();
    }

    return NextResponse.json({ success: true });
  } catch (error: any) {
    console.error('Error saving command result:', error);
    return NextResponse.json({ error: error.message || 'Internal server error' }, { status: 500 });
  }
}
