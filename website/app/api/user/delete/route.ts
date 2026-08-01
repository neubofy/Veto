import { NextResponse } from 'next/server';
import { adminDb, adminAuth } from '@/lib/firebaseAdmin';

export async function POST(req: Request) {
  try {
    const authHeader = req.headers.get('Authorization');
    const body = await req.json().catch(() => ({}));
    
    // Extract token from either Authorization header or request body
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

    const resultsSnap = await userRef.collection('results').get();
    const photosSnap = await userRef.collection('photos').get();
    const locationsSnap = await userRef.collection('locations').get();

    const batch = adminDb.batch();

    // Clear results
    resultsSnap.forEach((doc: any) => {
      batch.delete(doc.ref);
    });

    // Clear photos
    photosSnap.forEach((doc: any) => {
      batch.delete(doc.ref);
    });

    // Clear locations
    locationsSnap.forEach((doc: any) => {
      batch.delete(doc.ref);
    });

    // Delete the user document
    batch.delete(userRef);
    
    // Commit all Firestore deletions
    await batch.commit();

    // Finally, delete the Firebase Auth user
    await adminAuth.deleteUser(userId);

    return NextResponse.json({ success: true, message: 'Account and all data deleted successfully.' });

  } catch (error: any) {
    console.error('Error deleting account:', error);
    return NextResponse.json(
      { error: error.message || 'Internal server error' },
      { status: 500 }
    );
  }
}
