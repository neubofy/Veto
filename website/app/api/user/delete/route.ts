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
    const userRef = adminDb.collection('users').doc(userId);

    // 1. Recursively delete entire user document tree & all subcollections in Firebase
    try {
      await adminDb.recursiveDelete(userRef);
    } catch (e) {
      await userRef.delete();
    }

    // 2. Delete the Firebase Auth user account
    await adminAuth.deleteUser(userId);

    return NextResponse.json({ success: true, message: 'Account and all data permanently deleted' });
  } catch (error: any) {
    console.error('Error deleting user account:', error);
    return NextResponse.json({ error: error.message || 'Internal server error' }, { status: 500 });
  }
}
