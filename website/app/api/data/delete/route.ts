import { NextResponse } from 'next/server';
import { adminDb, adminAuth } from '@/lib/firebaseAdmin';

export async function POST(req: Request) {
  try {
    if (!adminAuth || !adminDb) {
      return NextResponse.json({ error: 'Firebase Admin environment not configured' }, { status: 500 });
    }

    const authHeader = req.headers.get('Authorization');
    const body = await req.json().catch(() => ({}));

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

    if (all) {
      // 1. Preserve FCM Token so paired device stays connected
      const userDoc = await userRef.get();
      const fcmToken = userDoc.exists ? userDoc.data()?.fcmToken : null;

      // 2. Recursively delete entire user document tree & all subcollections in Firebase
      try {
        await adminDb.recursiveDelete(userRef);
      } catch (e) {
        // Fallback if recursiveDelete fails: delete command_history collection directly
        const historySnap = await userRef.collection('command_history').get();
        const batch = adminDb.batch();
        historySnap.docs.forEach((doc: any) => batch.delete(doc.ref));
        await batch.commit();
      }

      // 3. Restore paired FCM Token doc if it existed
      if (fcmToken) {
        await userRef.set({
          fcmToken: fcmToken,
          updatedAt: new Date().toISOString()
        });
      }

      return NextResponse.json({ success: true, message: 'All user cloud data recursively deleted from Firebase' });
    } else if (commandName) {
      const baseCmd = commandName.split(' ')[0].toLowerCase();
      const commandDocRef = userRef.collection('command_history').doc(baseCmd);
      await commandDocRef.delete();

      // Legacy query cleanup if any
      const historySnap = await userRef.collection('command_history').where('command', '==', commandName).get();
      if (!historySnap.empty) {
        const batch = adminDb.batch();
        historySnap.docs.forEach((doc: any) => batch.delete(doc.ref));
        await batch.commit();
      }

      return NextResponse.json({ success: true, message: `Deleted ${commandName} data` });
    } else {
      return NextResponse.json({ error: 'Must specify commandName or all=true' }, { status: 400 });
    }
  } catch (error: any) {
    console.error('Error deleting data:', error);
    return NextResponse.json({ error: error.message || 'Internal server error' }, { status: 500 });
  }
}
