/*
 * $Id$
 * Copyright (C) 2001 The Apache Software Foundation. All rights reserved.
 * For details on use and redistribution please refer to the
 * LICENSE file included with these sources.
 */

package org.apache.fop.apps;

import org.apache.fop.messaging.MessageHandler;

public class Fop {
    public static void main(String[] args) {
        CommandLineOptions options = null;

        try {
            options = new CommandLineOptions(args);
            Starter starter = options.getStarter();
            starter.run();
        } catch (FOPException e) {
            MessageHandler.errorln("" + e.getMessage());
            if (options != null) {
              if (options.isDebugMode().booleanValue()) {
                e.printStackTrace();
              }
              if (options.outfile != null) {
                options.outfile.delete();
              }
            }
            System.exit(2);
        } catch (java.io.FileNotFoundException e) {
            MessageHandler.errorln("" + e.getMessage());
            if (options != null) {
              if (options.isDebugMode().booleanValue()) {
                e.printStackTrace();
              }
              if (options.outfile != null) {
                options.outfile.delete();
              }
            }
            System.exit(1);
        }
        System.exit(0);
    }

}

