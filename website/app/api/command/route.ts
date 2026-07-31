import { NextResponse } from 'next/server';
import { adminMessaging, adminDb, adminAuth } from '@/lib/firebaseAdmin';

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
    const { command } = body;

    if (!command) {
      return NextResponse.json({ error: 'Missing command' }, { status: 400 });
    }

    // Read the user's FCM token from Firestore (ensuring they can only command THEIR device)
    const userDoc = await adminDb.collection('users').doc(userId).get();
    
    if (!userDoc.exists) {
      return NextResponse.json({ error: 'User device not found in database' }, { status: 404 });
    }

    const fcmToken = userDoc.data()?.fcmToken;
    if (!fcmToken) {
      return NextResponse.json({ error: 'User does not have an FCM token registered' }, { status: 400 });
    }

    // Construct the data payload matching Android's VetoFirebaseMessagingService
    const message = {
      data: {
        command: command,
        uid: userId
      },
      token: fcmToken
    };

    // Send the push notification
    const response = await adminMessaging.send(message);

    return NextResponse.json({ success: true, messageId: response });
  } catch (error: any) {
    console.error('Error sending command:', error);
    return NextResponse.json({ error: error.message || 'Internal server error' }, { status: 500 });
  }
}
