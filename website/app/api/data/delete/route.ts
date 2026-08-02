import { NextResponse } from 'next/server';
import { adminDb, adminAuth } from '@/lib/firebaseAdmin';

export async function POST(req: Request) {
  try {
    if (!adminAuth || !adminDb) {
      return NextResponse.json({ error: 'Firebase Admin environment not configured' }, { status: 500 });
    }

    const authHeader = req.headers.get('Authorization');
    const body = await req.json().catch(() => ({}));
    
    // Support token from either Authorization header or request body
    let token = body.token;
    if (!token && authHeader?.startsWith('Bearer ')) {
      token = authHeader.split('Bearer ')[1];
    }

    if (!token) {
      return NextResponse.json({ error: 'Missing authentication token' }, { status: 401 });
    }

    let decodedToken;
    try {
      decodedToken = await adminAuth.verifyIdToken(token);
    } catch (e) {
      return NextResponse.json({ error: 'Invalid authentication token' }, { status: 401 });
    }

    const userId = decodedToken.uid;
    const { commandName, all } = body;
    const userRef = adminDb.collection('users').doc(userId);
    const batch = adminDb.batch();

    if (all) {
      // Delete all history documents
      const historySnap = await userRef.collection('command_history').get();
      historySnap.docs.forEach((doc: any) => {
        batch.delete(doc.ref);
      });
      // also cleanup legacy
      const resultsSnap = await userRef.collection('results').get();
      resultsSnap.docs.forEach((doc: any) => batch.delete(doc.ref));
      const photosSnap = await userRef.collection('photos').get();
      photosSnap.docs.forEach((doc: any) => batch.delete(doc.ref));
      const locationsSnap = await userRef.collection('locations').get();
      locationsSnap.docs.forEach((doc: any) => batch.delete(doc.ref));

    } else if (commandName) {
      // Delete specific command history
      const historySnap = await userRef.collection('command_history').where('command', '==', commandName).get();
      historySnap.docs.forEach((doc: any) => {
        batch.delete(doc.ref);
      });
      // legacy cleanup
      batch.delete(userRef.collection('results').doc(commandName));
      batch.delete(userRef.collection('photos').doc(commandName));
    } else {
      return NextResponse.json({ error: 'Must specify commandName or all=true' }, { status: 400 });
    }

    await batch.commit();

    return NextResponse.json({ success: true, message: 'Data deleted successfully from Firestore.' });
  } catch (error: any) {
    console.error('Error deleting data from Firestore:', error);
    return NextResponse.json({ error: error.message || 'Internal server error' }, { status: 500 });
  }
}
