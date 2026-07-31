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
    const { fcmToken } = body;

    if (!fcmToken) {
      return NextResponse.json({ error: 'Missing fcmToken' }, { status: 400 });
    }

    // Save the device's FCM token directly to the user's Firestore document
    // We use the authenticated userId from the token to ensure users can only update their own device
    await adminDb.collection('users').doc(userId).set({
      fcmToken: fcmToken,
      lastUpdated: new Date().toISOString()
    }, { merge: true });

    return NextResponse.json({ success: true });
  } catch (error: any) {
    console.error('Error linking device:', error);
    return NextResponse.json({ error: error.message || 'Internal server error' }, { status: 500 });
  }
}
