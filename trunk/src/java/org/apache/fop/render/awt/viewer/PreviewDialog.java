/*
 * Copyright 1999-2005 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/* $Id$ */

// Originally contributed by:
// Juergen Verwohlt: Juergen.Verwohlt@jCatalog.com,
// Rainer Steinkuhle: Rainer.Steinkuhle@jCatalog.com,
// Stanislav Gorkhover: Stanislav.Gorkhover@jCatalog.com
package org.apache.fop.render.awt.viewer;

// Java
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.print.PrinterException;
import java.awt.print.PrinterJob;

import java.text.DecimalFormat;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JComboBox;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JToolBar;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

import org.apache.fop.apps.FOUserAgent;
import org.apache.fop.apps.FOPException;
import org.apache.fop.render.awt.AWTRenderer;

/**
 * AWT Viewer main window.
 * Surrounds a PreviewPanel with a bunch of pretty buttons and controls.
 */
public class PreviewDialog extends JFrame {

    /** The Translator for localization */
    protected Translator translator;
    /** The AWT renderer */
    protected AWTRenderer renderer;
    /** The FOUserAgent associated with this window */
    protected FOUserAgent foUserAgent;
    /**
     * Renderable instance that can be used to reload and re-render a document after 
     * modifications.
     */
    protected Renderable renderable;

    /** The JCombobox to rescale the rendered page view */
    private JComboBox scale;

    /** The JLabel for the process status bar */
    private JLabel processStatus;

    /** The JLabel information status bar */
    private JLabel infoStatus;

    /** The main display area */
    private PreviewPanel previewPanel;

    /** Formats the text in the scale combobox. */
    private DecimalFormat percentFormat = new DecimalFormat("###0.0#");

    /**
     * Creates a new PreviewDialog that uses the given renderer.
     * @param foUserAgent the user agent
     * @param renderable the Renderable instance that is used to reload/re-render a document
     *                   after modifications.
     */
    public PreviewDialog(FOUserAgent foUserAgent, Renderable renderable) {
        renderer = (AWTRenderer) foUserAgent.getRendererOverride();
        this.foUserAgent = foUserAgent;
        this.renderable = renderable;
        translator = renderer.getTranslator();

        //Commands aka Actions
        Command printAction = new Command(translator.getString("Menu.Print"), "Print") {
            public void doit() {
                startPrinterJob(true);
            }
        };
        Command firstPageAction = new Command(translator.getString("Menu.First.page"),
                                      "firstpg") {
            public void doit() {
                goToFirstPage();
            }
        };
        Command previousPageAction = new Command(translator.getString("Menu.Prev.page"),
                                         "prevpg") {
            public void doit() {
                goToPreviousPage();
            }
        };
        Command nextPageAction = new Command(translator.getString("Menu.Next.page"), "nextpg") {
            public void doit() {
                goToNextPage();
            }

        };
        Command lastPageAction = new Command(translator.getString("Menu.Last.page"), "lastpg") {
            public void doit() {
                goToLastPage();
            }
        };
        Command reloadAction = new Command(translator.getString("Menu.Reload"), "reload") {
            public void doit() {
                previewPanel.reload();
            }
        };
        Command debugAction = new Command(translator.getString("Menu.Debug"), "debug") {
            // TODO use Translator
            public void doit() {
                previewPanel.debug();
            }
        };
        Command aboutAction = new Command(translator.getString("Menu.About"), "fopLogo") {
            public void doit() {
                startHelpAbout();
            }
        };

        //set the system look&feel
        try {
            UIManager.setLookAndFeel(
                UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            System.err.println("Couldn't set system look & feel!");
        }

        setTitle("FOP: AWT-" + translator.getString("Title.Preview"));
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        //Sets size to be 61%x90% of the screen size
        Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
        //Rather frivolous size - fits A4 page width in 1024x768 screen on my desktop
        setSize(screen.width * 61 / 100, screen.height * 9 / 10);

        //Page view stuff
        previewPanel = new PreviewPanel(foUserAgent, renderable, renderer);
        getContentPane().add(previewPanel, BorderLayout.CENTER);

        //Scaling combobox
        scale = new JComboBox();
        scale.addItem(translator.getString("Menu.Fit.Window"));
        scale.addItem(translator.getString("Menu.Fit.Width"));
        scale.addItem("25%");
        scale.addItem("50%");
        scale.addItem("75%");
        scale.addItem("100%");
        scale.addItem("150%");
        scale.addItem("200%");
        scale.setMaximumSize(new Dimension(80, 24));
        scale.setPreferredSize(new Dimension(80, 24));
        scale.setSelectedItem("100%");
        scale.setEditable(true);
        scale.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                scaleActionPerformed(e);
            }
        });

        //Menu
        setJMenuBar(setupMenu());

        // Toolbar
        JToolBar toolBar = new JToolBar();
        toolBar.add(printAction);
        toolBar.add(reloadAction);
        toolBar.addSeparator();
        toolBar.add(firstPageAction);
        toolBar.add(previousPageAction);
        toolBar.add(nextPageAction);
        toolBar.add(lastPageAction);
        toolBar.addSeparator(new Dimension(20, 0));
        toolBar.add(new JLabel(translator.getString("Menu.Zoom") + " "));
        toolBar.add(scale);
        toolBar.addSeparator();
        toolBar.add(debugAction);
        toolBar.addSeparator();
        toolBar.add(aboutAction);
        getContentPane().add(toolBar, BorderLayout.NORTH);

        // Status bar
        JPanel statusBar = new JPanel();
        processStatus = new JLabel();
        processStatus.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createEtchedBorder(),
                BorderFactory.createEmptyBorder(0, 3, 0, 0)));
        infoStatus = new JLabel();
        infoStatus.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createEtchedBorder(),
                BorderFactory.createEmptyBorder(0, 3, 0, 0)));

        statusBar.setLayout(new GridBagLayout());

        processStatus.setPreferredSize(new Dimension(200, 21));
        processStatus.setMinimumSize(new Dimension(200, 21));

        infoStatus.setPreferredSize(new Dimension(100, 21));
        infoStatus.setMinimumSize(new Dimension(100, 21));
        statusBar.add(processStatus,
                      new GridBagConstraints(0, 0, 1, 0, 2.0, 0.0,
                                             GridBagConstraints.CENTER,
                                             GridBagConstraints.HORIZONTAL,
                                             new Insets(0, 0, 0, 3), 0, 0));
        statusBar.add(infoStatus,
                      new GridBagConstraints(1, 0, 1, 0, 1.0, 0.0,
                                             GridBagConstraints.CENTER,
                                             GridBagConstraints.HORIZONTAL,
                                             new Insets(0, 0, 0, 0), 0, 0));
        getContentPane().add(statusBar, BorderLayout.SOUTH);
    }

    /**
     * Creates a new PreviewDialog that uses the given renderer.
     * @param foUserAgent the user agent
     */
    public PreviewDialog(FOUserAgent foUserAgent) {
        this(foUserAgent, null);
    }
    
    /**
     * Creates a new menubar to be shown in this window.
     * @return the newly created menubar
     */
    private JMenuBar setupMenu() {
        JMenuBar menuBar = new JMenuBar();
        JMenu menu = new JMenu(translator.getString("Menu.File"));
        menu.setMnemonic(KeyEvent.VK_F);
        //Adds mostly the same actions, but without icons
        menu.add(new Command(translator.getString("Menu.Print"), KeyEvent.VK_P) {
            public void doit() {
                startPrinterJob(true);
            }
        });
        // inputHandler must be set to allow reloading
        if (renderable != null) {
            menu.add(new Command(translator.getString("Menu.Reload"), KeyEvent.VK_R) {
                public void doit() {
                    reload();
                }
            });
        }
        menu.addSeparator();
        menu.add(new Command(translator.getString("Menu.Exit"), KeyEvent.VK_X) {
            public void doit() {
                dispose();
            }
        });
        menuBar.add(menu);

        menu = new JMenu(translator.getString("Menu.View"));
        menu.setMnemonic(KeyEvent.VK_V);
        menu.add(new Command(translator.getString("Menu.First.page"), KeyEvent.VK_F) {
            public void doit() {
                goToFirstPage();
            }
        });
        menu.add(new Command(translator.getString("Menu.Prev.page"), KeyEvent.VK_P) {
            public void doit() {
                goToPreviousPage();
            }
        });
        menu.add(new Command(translator.getString("Menu.Next.page"), KeyEvent.VK_N) {
            public void doit() {
                goToNextPage();
            }
        });
        menu.add(new Command(translator.getString("Menu.Last.page"), KeyEvent.VK_L) {
            public void doit() {
                goToLastPage();
            }
        });
        menu.add(new Command(translator.getString("Menu.Go.to.Page"), KeyEvent.VK_G) {
            public void doit() {
                showGoToPageDialog();
            }
        });
        menu.addSeparator();
        JMenu subMenu = new JMenu(translator.getString("Menu.Zoom"));
        subMenu.setMnemonic(KeyEvent.VK_Z);
        subMenu.add(new Command("25%", 0) {
            public void doit() {
                setScale(25.0);
            }
        });
        subMenu.add(new Command("50%", 0) {
            public void doit() {
                setScale(50.0);
            }
        });
        subMenu.add(new Command("75%", 0) {
            public void doit() {
                setScale(75.0);
            }
        });
        subMenu.add(new Command("100%", 0) {
            public void doit() {
                setScale(100.0);
            }
        });
        subMenu.add(new Command("150%", 0) {
            public void doit() {
                setScale(150.0);
            }
        });
        subMenu.add(new Command("200%", 0) {
            public void doit() {
                setScale(200.0);
            }
        });
        menu.add(subMenu);
        menu.addSeparator();
        menu.add(new Command(translator.getString("Menu.Default.zoom"), KeyEvent.VK_D) {
            public void doit() {
                setScale(100.0);
            }
        });
        menu.add(new Command(translator.getString("Menu.Fit.Window"), KeyEvent.VK_F) {
            public void doit() {
                setScaleToFitWindow();
            }
        });
        menu.add(new Command(translator.getString("Menu.Fit.Width"), KeyEvent.VK_W) {
            public void doit() {
                setScaleToFitWidth();
            }
        });
        menu.addSeparator();

        ButtonGroup group = new ButtonGroup();
        JRadioButtonMenuItem single = new JRadioButtonMenuItem(
                new Command(translator.getString("Menu.Single"), KeyEvent.VK_S) {
            public void doit() {
                previewPanel.setDisplayMode(PreviewPanel.SINGLE);
            }
        });
        JRadioButtonMenuItem cont = new JRadioButtonMenuItem(
                new Command(translator.getString("Menu.Continuous"), KeyEvent.VK_C) {
            public void doit() {
                previewPanel.setDisplayMode(PreviewPanel.CONTINUOUS);
            }
        });
        JRadioButtonMenuItem facing = new JRadioButtonMenuItem(
                new Command(translator.getString("Menu.Facing"), 0) {
            public void doit() {
                previewPanel.setDisplayMode(PreviewPanel.CONT_FACING);
            }
        });
        single.setSelected(true);
        group.add(single);
        group.add(cont);
        group.add(facing);
        menu.add(single);
        menu.add(cont);
        menu.add(facing);

        menuBar.add(menu);

        menu = new JMenu(translator.getString("Menu.Help"));
        menu.setMnemonic(KeyEvent.VK_H);
        menu.add(new Command(translator.getString("Menu.About"), KeyEvent.VK_A) {
            public void doit() {
                startHelpAbout();
            }
        });
        menuBar.add(menu);
        return menuBar;
    }

    public void reload() {
        previewPanel.reload();
    }

    /**
     * Changes the current visible page
     * @param number the page number to go to
     */
    public void goToPage(int number) {
        if (number != previewPanel.getPage()) {
            previewPanel.setPage(number);
            setInfo();
        }
    }

    /**
     * Shows the previous page.
     */
    public void goToPreviousPage() {
        int page = previewPanel.getPage();
        if (page > 0) {
            goToPage(page - 1);
        }
    }

    /**
     * Shows the next page.
     */
    public void goToNextPage() {
        int page = previewPanel.getPage();
        if (page < renderer.getNumberOfPages() - 1) {
            goToPage(page + 1);
        }
    }

    /** Shows the first page. */
    public void goToFirstPage() {
        goToPage(0);
    }

    /**
     * Shows the last page.
     */
    public void goToLastPage() {
        goToPage(renderer.getNumberOfPages() - 1);
    }

    /** Shows the About box */
    private void startHelpAbout() {
        PreviewDialogAboutBox dlg = new PreviewDialogAboutBox(this, translator);
        //Centers the box
        Dimension dlgSize = dlg.getPreferredSize();
        Dimension frmSize = getSize();
        Point loc = getLocation();
        dlg.setLocation((frmSize.width - dlgSize.width) / 2 + loc.x,
                        (frmSize.height - dlgSize.height) / 2 + loc.y);
        dlg.setVisible(true);
    }

    /**
     * Shows "go to page" dialog and then goes to the selected page
     */
    private void showGoToPageDialog() {
                int currentPage = previewPanel.getPage();
        GoToPageDialog d = new GoToPageDialog(this,
            translator.getString("Menu.Go.to.Page"), translator);
        d.setLocation((int)getLocation().getX() + 50,
                      (int)getLocation().getY() + 50);
        d.setVisible(true);
        currentPage = d.getPageNumber();
        if (currentPage < 1 || currentPage > renderer.getNumberOfPages()) {
            return;
        }
        currentPage--;
        goToPage(currentPage);
    }

    /** Scales page image */
    public void setScale(double scaleFactor) {
//         if (scaleFactor == 25.0) {
//             scale.setSelectedIndex(0);
//         } else if (scaleFactor == 50.0) {
//             scale.setSelectedIndex(1);
//         } else if (scaleFactor == 75.0) {
//             scale.setSelectedIndex(2);
//         } else if (scaleFactor == 100.0) {
//             scale.setSelectedIndex(3);
//         } else if (scaleFactor == 150.0) {
//             scale.setSelectedIndex(4);
//         } else if (scaleFactor == 200.0) {
//             scale.setSelectedIndex(5);
//         } else if (scaleFactor == 400.0) {
//             scale.setSelectedIndex(6);
//         } else {
        scale.setSelectedItem(percentFormat.format(scaleFactor) + "%");
//              }
        previewPanel.setScaleFactor(scaleFactor / 100d);
    }

    public void setScaleToFitWindow() {
        try {
            setScale(previewPanel.getScaleToFitWindow() * 100);
        } catch (FOPException fopEx) {
            fopEx.printStackTrace();
        }
    }
    
    public void setScaleToFitWidth() {
        try {
            setScale(previewPanel.getScaleToFitWidth() * 100);
        } catch (FOPException fopEx) {
            fopEx.printStackTrace();
        }
    }

    private void scaleActionPerformed(ActionEvent e) {
        try {
            int index = scale.getSelectedIndex();
            if (index == 0) {
                setScale(previewPanel.getScaleToFitWindow() * 100);
            } else if (index == 1) {
                setScale(previewPanel.getScaleToFitWidth() * 100);
            } else {
                String item = (String)scale.getSelectedItem();
                setScale(Double.parseDouble(item.substring(0, item.indexOf('%'))));
            }
        } catch (FOPException fopEx) {
            fopEx.printStackTrace();
        }
    }

    /** Prints the document */
    public void startPrinterJob(boolean showDialog) {
        PrinterJob pj = PrinterJob.getPrinterJob();
        pj.setPageable(renderer);
        if (!showDialog || pj.printDialog()) {
            try {
                pj.print();
            } catch (PrinterException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Sets message to be shown in the status bar in a thread safe way.
     * @param message the message
     */
    public void setStatus(String message) {
        SwingUtilities.invokeLater(new ShowStatus(message));
    }

    /** This class is used to show status in a thread safe way. */
    private class ShowStatus implements Runnable {

        /** The message to display */
        private String message;

        /**
         * Constructs ShowStatus thread
         * @param message message to display
         */
        public ShowStatus(String message) {
            this.message = message;
        }

        public void run() {
            processStatus.setText(message.toString());
        }
    }

    /**
     * Updates the message to be shown in the info bar in a thread safe way.
     */
    public void setInfo() {
        SwingUtilities.invokeLater(new ShowInfo());
    }

    /** This class is used to show info in a thread safe way. */
    private class ShowInfo implements Runnable {

        public void run() {
            String message = translator.getString("Status.Page") + " "
                    + (previewPanel.getPage() + 1) + " "
                    + translator.getString("Status.of") + " "
                    + (renderer.getNumberOfPages());
            infoStatus.setText(message);
        }
    }

    /**
     * Opens standard Swing error dialog box and reports given exception details.
     * @param e the Exception
     */
    public void reportException(Exception e) {
        String msg = translator.getString("Exception.Occured");
        setStatus(msg);
        JOptionPane.showMessageDialog(
            getContentPane(),
            "<html><b>" + msg + ":</b><br>"
                + e.getClass().getName() + "<br>"
                + e.getMessage() + "</html>",
            translator.getString("Exception.Error"),
            JOptionPane.ERROR_MESSAGE
        );
    }
}
