import { NextResponse } from 'next/server';
import { adminDb, adminAuth } from '@/lib/firebaseAdmin';

export async function POST(req: Request) {
  try {
    const { token, commandName, all } = await req.json();

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
    const batch = adminDb.batch();

    if (all) {
      // Delete all results
      const resultsSnap = await userRef.collection('results').get();
      resultsSnap.docs.forEach((doc: any) => {
        batch.delete(doc.ref);
      });

      // Delete all photos
      const photosSnap = await userRef.collection('photos').get();
      photosSnap.docs.forEach((doc: any) => {
        batch.delete(doc.ref);
      });
      
      // We don't delete from Vercel Blob here since we are using Google Drive now,
      // but if there were old Vercel Blobs we could try to delete them by looking at the data
    } else if (commandName) {
      // Delete specific result
      const resultDoc = userRef.collection('results').doc(commandName);
      batch.delete(resultDoc);

      // Delete specific photo
      const photoDoc = userRef.collection('photos').doc(commandName);
      batch.delete(photoDoc);
    } else {
      return NextResponse.json({ error: 'Must specify commandName or all=true' }, { status: 400 });
    }

    await batch.commit();

    return NextResponse.json({ success: true, message: 'Data deleted successfully.' });
  } catch (error: any) {
    console.error('Error deleting data:', error);
    return NextResponse.json({ error: error.message || 'Internal server error' }, { status: 500 });
  }
}
