import { describe, it, expect, beforeEach } from 'vitest';
import { useSidebarStore } from './sidebar-store';

describe('sidebar-store', () => {
  beforeEach(() => {
    // Reset store to initial state before each test
    useSidebarStore.setState({ isOpen: true, isCollapsed: false });
  });

  describe('initial state', () => {
    it('starts with isOpen as true', () => {
      const { isOpen } = useSidebarStore.getState();
      expect(isOpen).toBe(true);
    });

    it('starts with isCollapsed as false', () => {
      const { isCollapsed } = useSidebarStore.getState();
      expect(isCollapsed).toBe(false);
    });
  });

  describe('toggle', () => {
    it('toggles isOpen from true to false', () => {
      const { toggle } = useSidebarStore.getState();
      toggle();
      expect(useSidebarStore.getState().isOpen).toBe(false);
    });

    it('toggles isOpen from false to true', () => {
      useSidebarStore.setState({ isOpen: false });
      const { toggle } = useSidebarStore.getState();
      toggle();
      expect(useSidebarStore.getState().isOpen).toBe(true);
    });

    it('toggles multiple times correctly', () => {
      const { toggle } = useSidebarStore.getState();
      toggle(); // true -> false
      expect(useSidebarStore.getState().isOpen).toBe(false);
      toggle(); // false -> true
      expect(useSidebarStore.getState().isOpen).toBe(true);
      toggle(); // true -> false
      expect(useSidebarStore.getState().isOpen).toBe(false);
    });
  });

  describe('open', () => {
    it('sets isOpen to true', () => {
      useSidebarStore.setState({ isOpen: false });
      const { open } = useSidebarStore.getState();
      open();
      expect(useSidebarStore.getState().isOpen).toBe(true);
    });

    it('keeps isOpen as true when already true', () => {
      useSidebarStore.setState({ isOpen: true });
      const { open } = useSidebarStore.getState();
      open();
      expect(useSidebarStore.getState().isOpen).toBe(true);
    });
  });

  describe('close', () => {
    it('sets isOpen to false', () => {
      useSidebarStore.setState({ isOpen: true });
      const { close } = useSidebarStore.getState();
      close();
      expect(useSidebarStore.getState().isOpen).toBe(false);
    });

    it('keeps isOpen as false when already false', () => {
      useSidebarStore.setState({ isOpen: false });
      const { close } = useSidebarStore.getState();
      close();
      expect(useSidebarStore.getState().isOpen).toBe(false);
    });
  });

  describe('setCollapsed', () => {
    it('sets isCollapsed to true', () => {
      const { setCollapsed } = useSidebarStore.getState();
      setCollapsed(true);
      expect(useSidebarStore.getState().isCollapsed).toBe(true);
    });

    it('sets isCollapsed to false', () => {
      useSidebarStore.setState({ isCollapsed: true });
      const { setCollapsed } = useSidebarStore.getState();
      setCollapsed(false);
      expect(useSidebarStore.getState().isCollapsed).toBe(false);
    });

    it('updates isCollapsed independently of isOpen', () => {
      useSidebarStore.setState({ isOpen: false, isCollapsed: false });
      const { setCollapsed } = useSidebarStore.getState();
      setCollapsed(true);
      expect(useSidebarStore.getState().isCollapsed).toBe(true);
      expect(useSidebarStore.getState().isOpen).toBe(false);
    });
  });
});
