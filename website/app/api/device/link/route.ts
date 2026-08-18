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

    // Allow an explicitly falsy fcmToken to signify unlinking
    const hasFcmTokenField = 'fcmToken' in body;
    const fcmToken = body.fcmToken;

    if (!hasFcmTokenField) {
      return NextResponse.json({ error: 'Missing fcmToken' }, { status: 400 });
    }

    if (fcmToken) {
      // Save the device's FCM token directly to the user's Firestore document
      await adminDb.collection('users').doc(userId).set({
        fcmToken: fcmToken,
        lastUpdated: new Date().toISOString()
      }, { merge: true });
    } else {
      // Remove the fcmToken field if it's empty/null (sign out)
      const FieldValue = (await import('firebase-admin/firestore')).FieldValue;
      await adminDb.collection('users').doc(userId).set({
        fcmToken: FieldValue.delete(),
        lastUpdated: new Date().toISOString()
      }, { merge: true });
    }

    return NextResponse.json({ success: true });
  } catch (error: any) {
    console.error('Error linking device:', error);
    return NextResponse.json({ error: error.message || 'Internal server error' }, { status: 500 });
  }
}
