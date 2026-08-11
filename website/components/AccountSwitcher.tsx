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

  return (
    <div className="relative" ref={dropdownRef}>
      <button
        onClick={() => setIsOpen(!isOpen)}
        className="flex items-center gap-3 rounded-full bg-gray-800/50 py-1.5 pl-1.5 pr-4 ring-1 ring-white/10 transition-all hover:bg-gray-800 hover:ring-white/20"
      >
        <img
          src={currentAccount.photoURL || `https://ui-avatars.com/api/?name=${encodeURIComponent(currentAccount.displayName)}`}
          alt=""
          className="h-8 w-8 rounded-full object-cover"
        />
        <div className="flex flex-col items-start">
          <span className="text-sm font-medium text-white">{currentAccount.displayName}</span>
          <span className="text-[10px] text-green-400 font-medium">● Connected</span>
        </div>
        <ChevronDown className={`ml-2 h-4 w-4 text-gray-400 transition-transform ${isOpen ? 'rotate-180' : ''}`} />
      </button>

      <AnimatePresence>
        {isOpen && (
          <motion.div
            initial={{ opacity: 0, y: 10, scale: 0.95 }}
            animate={{ opacity: 1, y: 0, scale: 1 }}
            exit={{ opacity: 0, y: 10, scale: 0.95 }}
            transition={{ duration: 0.15 }}
            className="absolute right-0 mt-2 w-80 origin-top-right overflow-hidden rounded-2xl bg-gray-900 border border-gray-800 shadow-2xl shadow-black/50 z-50"
          >
            <div className="py-2">
              {accounts.map((account) => {
                const isCurrent = account.uid === currentAccount.uid;
                
                return (
                  <div
                    key={account.uid}
                    onClick={() => handleSwitch(account)}
                    className={`group relative flex cursor-pointer flex-col gap-2 px-4 py-3 hover:bg-gray-800/50 transition-colors ${
                      isCurrent ? 'bg-blue-500/10' : ''
                    }`}
                  >
                    <div className="flex items-center justify-between">
                      <div className="flex items-center gap-3">
                        <div className="relative">
                          <img
                            src={account.photoURL || `https://ui-avatars.com/api/?name=${encodeURIComponent(account.displayName)}`}
                            alt=""
                            className="h-10 w-10 rounded-full object-cover"
                          />
                          {isCurrent && (
                            <div className="absolute -bottom-1 -right-1 flex h-4 w-4 items-center justify-center rounded-full bg-blue-500 ring-2 ring-gray-900">
                              <Check className="h-2.5 w-2.5 text-white" />
                            </div>
                          )}
                        </div>
                        <div className="flex flex-col">
                          <span className="text-sm font-medium text-white flex items-center gap-2">
                            {account.displayName}
                            {account.isDefault && <Star className="h-3 w-3 fill-yellow-500 text-yellow-500" />}
                          </span>
                          <span className="text-xs text-gray-400">{account.email}</span>
                        </div>
                      </div>
                      
                      {!isCurrent && (
                        <span className="text-xs font-medium text-blue-400 opacity-0 group-hover:opacity-100 transition-opacity">
                          Switch →
                        </span>
                      )}
                    </div>
                    
                    <div className="ml-13 flex gap-3 opacity-0 group-hover:opacity-100 transition-opacity">
                      {!account.isDefault && (
                        <button
                          onClick={(e) => handleSetDefault(e, account.uid)}
                          className="text-xs font-medium text-gray-400 hover:text-white"
                        >
                          Set Default
                        </button>
                      )}
                      <button
                        onClick={(e) => handleRemove(e, account.uid)}
                        className="text-xs font-medium text-red-400 hover:text-red-300 flex items-center gap-1"
                      >
                        <Trash2 className="h-3 w-3" /> Remove
                      </button>
                    </div>
                  </div>
                );
              })}
            </div>
            
            <div className="border-t border-gray-800 p-2">
              <button
                onClick={handleAddAccount}
                className="flex w-full items-center gap-3 rounded-xl px-3 py-2 text-sm font-medium text-gray-300 hover:bg-gray-800 hover:text-white transition-colors"
              >
                <Plus className="h-4 w-4 text-gray-400" />
                Add New Account
              </button>
              
              <button
                onClick={onLogoutCurrent}
                className="flex w-full items-center gap-3 rounded-xl px-3 py-2 text-sm font-medium text-gray-300 hover:bg-gray-800 hover:text-white transition-colors mt-1"
              >
                <LogOut className="h-4 w-4 text-gray-400" />
                Logout Current
              </button>
            </div>
          </motion.div>
        )}
      </AnimatePresence>
    </div>
  );
}
