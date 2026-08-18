import { NextResponse } from 'next/server';
import { adminMessaging, adminDb, adminAuth } from '@/lib/firebaseAdmin';

export async function POST(req: Request) {
  try {
    if (!adminAuth || !adminDb || !adminMessaging) {
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
    const { command, encrypted, fcmToken } = body;

    if (!command) {
      return NextResponse.json({ error: 'Missing command' }, { status: 400 });
    }

    if (!fcmToken) {
      return NextResponse.json({ error: 'Missing fcmToken. Token must be decrypted client-side and provided.' }, { status: 400 });
    }

    if (!encrypted) {
      return NextResponse.json({ error: 'Only encrypted commands are accepted.' }, { status: 400 });
    }

    // Construct the data payload matching Android's VetoFirebaseMessagingService
    const dataPayload: Record<string, string> = {};

    dataPayload['encryptedCommand'] = command;

    const message = {
      data: dataPayload,
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
