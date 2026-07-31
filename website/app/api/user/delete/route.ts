import { NextResponse } from 'next/server';
import { adminDb, adminAuth } from '@/lib/firebaseAdmin';
import { del } from '@vercel/blob';

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
    const userRef = adminDb.collection('users').doc(userId);

    const resultsSnap = await userRef.collection('results').get();
    const photosSnap = await userRef.collection('photos').get();

    const batch = adminDb.batch();

    // Clear results
    resultsSnap.forEach((doc: any) => {
      batch.delete(doc.ref);
    });

    // Clear photos and blobs
    const deletePromises = [];
    for (const doc of photosSnap.docs) {
      const data = doc.data();
      if (data?.url) {
        deletePromises.push(
          del(data.url).catch(e => console.error('Failed to delete blob', data.url, e))
        );
      }
      batch.delete(doc.ref);
    }
    await Promise.all(deletePromises);

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
