package edu.cmu.cs.fluid.control;

public enum WhichPort {
    ENTRY {
      @Override
      public ComponentPort getPort(final Component c) {
        return c.getEntryPort();
      }
      @Override
      public boolean isExitPort() { return false; }
    },
    
    NORMAL_EXIT {
      @Override
      public ComponentPort getPort(final Component c) {
        return c.getNormalExitPort();
      }
    },

    ABRUPT_EXIT {
      @Override
      public ComponentPort getPort(final Component c) {
        return c.getAbruptExitPort();
      }
    };
    
    public abstract ComponentPort getPort(Component c);
    public boolean isExitPort() { return true; }
  }