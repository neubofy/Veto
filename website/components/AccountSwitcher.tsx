'use client';

import React, { useState, useRef, useEffect } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import { ChevronDown, Check, Star, LogOut, Plus, Trash2 } from 'lucide-react';
import { accountManager, StoredAccount } from '@/lib/accountManager';
import { auth } from '@/lib/firebaseClient';
import { GoogleAuthProvider, signInWithPopup } from 'firebase/auth';

interface AccountSwitcherProps {
  currentAccount: StoredAccount | null;
  onAccountSwitch: (account: StoredAccount) => void;
  onLogoutCurrent: () => void;
}

export default function AccountSwitcher({ currentAccount, onAccountSwitch, onLogoutCurrent }: AccountSwitcherProps) {
  const [isOpen, setIsOpen] = useState(false);
  const [accounts, setAccounts] = useState<StoredAccount[]>([]);
  const dropdownRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    setAccounts(accountManager.getStoredAccounts());
    
    const handleClickOutside = (event: MouseEvent) => {
      if (dropdownRef.current && !dropdownRef.current.contains(event.target as Node)) {
        setIsOpen(false);
      }
    };
    
    document.addEventListener('mousedown', handleClickOutside);
    return () => document.removeEventListener('mousedown', handleClickOutside);
  }, [isOpen]);

  const handleSwitch = async (account: StoredAccount) => {
    if (currentAccount?.uid === account.uid) {
      setIsOpen(false);
      return;
    }
    
    try {
      const provider = new GoogleAuthProvider();
      provider.setCustomParameters({ prompt: 'select_account' });
      await signInWithPopup(auth, provider);
      onAccountSwitch(account);
      setIsOpen(false);
    } catch (e) {
      console.error('Failed to switch account:', e);
    }
  };

  const handleAddAccount = async () => {
    try {
      const provider = new GoogleAuthProvider();
      provider.setCustomParameters({ prompt: 'select_account' });
      const result = await signInWithPopup(auth, provider);
      
      const newAccount: Omit<StoredAccount, 'isDefault'> = {
        uid: result.user.uid,
        email: result.user.email || '',
        displayName: result.user.displayName || 'Unknown User',
        photoURL: result.user.photoURL || '',
      };
      
      accountManager.addAccount(newAccount);
      setAccounts(accountManager.getStoredAccounts());
      
      const stored = accountManager.getStoredAccounts().find(a => a.uid === result.user.uid);
      if (stored) {
        onAccountSwitch(stored);
      }
      setIsOpen(false);
    } catch (e) {
      console.error('Failed to add account:', e);
    }
  };

  const handleRemove = (e: React.MouseEvent, uid: string) => {
    e.stopPropagation();
    if (confirm('Are you sure you want to remove this account?')) {
      accountManager.removeAccount(uid);
      setAccounts(accountManager.getStoredAccounts());
      if (currentAccount?.uid === uid) {
        onLogoutCurrent();
      }
    }
  };

  const handleSetDefault = (e: React.MouseEvent, uid: string) => {
    e.stopPropagation();
    accountManager.setDefaultAccount(uid);
    setAccounts(accountManager.getStoredAccounts());
  };

  if (!currentAccount) return null;

  const otherAccounts = accounts.filter(a => a.uid !== currentAccount.uid);

  return (
    <div className="relative" ref={dropdownRef}>
      <button
        onClick={() => setIsOpen(!isOpen)}
        className="flex items-center justify-center rounded-full p-1 transition-all hover:bg-gray-800 focus:outline-none focus:ring-2 focus:ring-blue-500"
      >
        <img
          src={currentAccount.photoURL || `https://ui-avatars.com/api/?name=${encodeURIComponent(currentAccount.displayName)}`}
          alt="Profile"
          className="h-9 w-9 rounded-full object-cover ring-2 ring-gray-700 hover:ring-gray-500 transition-all"
        />
      </button>

      <AnimatePresence>
        {isOpen && (
          <motion.div
            initial={{ opacity: 0, y: 10, scale: 0.95 }}
            animate={{ opacity: 1, y: 0, scale: 1 }}
            exit={{ opacity: 0, y: 10, scale: 0.95 }}
            transition={{ duration: 0.15 }}
            className="absolute right-0 mt-3 w-[360px] origin-top-right overflow-hidden rounded-3xl bg-[#1e1f22] border border-gray-800/60 shadow-2xl z-50 sm:w-[400px]"
          >
            {/* Current Account Header */}
            <div className="flex flex-col items-center justify-center p-6 bg-[#1e1f22]">
              <div className="relative mb-3">
                <img
                  src={currentAccount.photoURL || `https://ui-avatars.com/api/?name=${encodeURIComponent(currentAccount.displayName)}`}
                  alt=""
                  className="h-20 w-20 rounded-full object-cover ring-4 ring-gray-800"
                />
              </div>
              <span className="text-xl font-medium text-gray-100">{currentAccount.displayName}</span>
              <span className="text-sm text-gray-400 mb-5">{currentAccount.email}</span>
              
              <button 
                onClick={() => {}} 
                className="rounded-full border border-gray-600 px-6 py-2 text-sm font-medium text-gray-200 transition-colors hover:bg-gray-800"
              >
                Manage your Veto Account
              </button>
            </div>

            {/* Other Accounts List */}
            {otherAccounts.length > 0 && (
              <div className="border-t border-gray-800 py-2">
                <div className="max-h-60 overflow-y-auto">
                  {otherAccounts.map((account) => (
                    <div
                      key={account.uid}
                      onClick={() => handleSwitch(account)}
                      className="group flex cursor-pointer items-center justify-between px-6 py-3 transition-colors hover:bg-gray-800/60"
                    >
                      <div className="flex items-center gap-4">
                        <img
                          src={account.photoURL || `https://ui-avatars.com/api/?name=${encodeURIComponent(account.displayName)}`}
                          alt=""
                          className="h-9 w-9 rounded-full object-cover"
                        />
                        <div className="flex flex-col">
                          <span className="text-sm font-medium text-gray-200 group-hover:text-white">
                            {account.displayName}
                          </span>
                          <span className="text-xs text-gray-500 group-hover:text-gray-400">
                            {account.email}
                          </span>
                        </div>
                      </div>
                      <div className="flex items-center gap-2 opacity-0 group-hover:opacity-100 transition-opacity">
                        <button
                          onClick={(e) => handleRemove(e, account.uid)}
                          className="rounded p-2 text-gray-500 hover:bg-gray-700 hover:text-red-400"
                          title="Remove account"
                        >
                          <Trash2 className="h-4 w-4" />
                        </button>
                      </div>
                    </div>
                  ))}
                </div>
              </div>
            )}

            {/* Actions */}
            <div className="border-t border-gray-800 py-2">
              <button
                onClick={handleAddAccount}
                className="flex w-full items-center gap-4 px-6 py-3 text-sm font-medium text-gray-300 transition-colors hover:bg-gray-800/60"
              >
                <div className="flex h-8 w-8 items-center justify-center rounded-full bg-gray-800">
                  <Plus className="h-4 w-4 text-gray-300" />
                </div>
                Add another account
              </button>
              
              <button
                onClick={onLogoutCurrent}
                className="flex w-full items-center gap-4 px-6 py-3 text-sm font-medium text-gray-300 transition-colors hover:bg-gray-800/60"
              >
                <div className="flex h-8 w-8 items-center justify-center rounded-full bg-gray-800">
                  <LogOut className="h-4 w-4 text-gray-300" />
                </div>
                Sign out
              </button>
            </div>
            
            {/* Footer */}
            <div className="border-t border-gray-800 py-3 flex justify-center gap-4 text-xs text-gray-500">
              <a href="/privacy" className="hover:text-gray-300">Privacy Policy</a>
              <span>•</span>
              <a href="/terms" className="hover:text-gray-300">Terms of Service</a>
            </div>
          </motion.div>
        )}
      </AnimatePresence>
    </div>
  );
}
