import { initializeApp, getApps, cert, getApp } from 'firebase-admin/app';
import { getFirestore } from 'firebase-admin/firestore';
import { getMessaging } from 'firebase-admin/messaging';
import { getAuth } from 'firebase-admin/auth';
import { getStorage } from 'firebase-admin/storage';

if (!getApps().length) {
  try {
    // These environment variables will be populated in Vercel securely
    const privateKey = process.env.FIREBASE_PRIVATE_KEY
      ? process.env.FIREBASE_PRIVATE_KEY.replace(/\\n/g, '\n')
      : undefined;

    if (process.env.FIREBASE_PROJECT_ID) {
      initializeApp({
        credential: cert({
          projectId: process.env.FIREBASE_PROJECT_ID,
          clientEmail: process.env.FIREBASE_CLIENT_EMAIL,
          privateKey: privateKey,
        }),
        storageBucket: `${process.env.FIREBASE_PROJECT_ID}.appspot.com`
      });
    } else {
      console.warn('Skipping Firebase Admin initialization: FIREBASE_PROJECT_ID is missing.');
    }
  } catch (error) {
    console.error('Firebase Admin Initialization Error', error);
  }
}

export const adminDb = getApps().length ? getFirestore(getApp(), '(default)') : null as any;
export const adminMessaging = getApps().length ? getMessaging() : null as any;
export const adminAuth = getApps().length ? getAuth() : null as any;
export const adminStorage = getApps().length ? getStorage() : null as any;
