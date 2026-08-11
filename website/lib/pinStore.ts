let inMemoryPin: string | null = null; export const pinStore = { get: () => inMemoryPin, set: (pin: string | null) => { inMemoryPin = pin; } };
