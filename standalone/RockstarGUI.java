import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.util.*;
import java.util.List;

public class RockstarGUI extends JFrame {

    // ─── Paleta kolorow ───────────────────────────────────────────
    static final Color BG           = new Color(0x0D, 0x0F, 0x16);
    static final Color SIDEBAR      = new Color(0x0F, 0x11, 0x18);
    static final Color CARD         = new Color(0x13, 0x15, 0x1F);
    static final Color CARD_ON      = new Color(0x17, 0x1E, 0x2D);
    static final Color CARD_BORDER  = new Color(0x22, 0x27, 0x36);
    static final Color ACCENT       = new Color(0x00, 0xE5, 0xFF);
    static final Color ACCENT2      = new Color(0x00, 0xC0, 0xFA);
    static final Color TEXT_MAIN    = new Color(0xFF, 0xFF, 0xFF);
    static final Color TEXT_DIM     = new Color(0x65, 0x6D, 0x84);
    static final Color TEXT_MID     = new Color(0xBD, 0xC4, 0xD4);
    static final Color SEP          = new Color(0x20, 0x25, 0x34);

    // ─── Model modulow ────────────────────────────────────────────
    enum Category {
        COMBAT("Combat", new Color(0xEF,0x53,0x50)),
        MOVEMENT("Movement", new Color(0x66,0xBB,0x6A)),
        RENDER("Render", new Color(0xAB,0x47,0xBC)),
        PLAYER("Player", new Color(0xFF,0xA7,0x26)),
        MISC("Misc", new Color(0x26,0xC6,0xDA));

        final String name; final Color color;
        Category(String n, Color c){ name=n; color=c; }
    }

    static class Setting {
        enum Type { TOGGLE, SLIDER, MODE }
        String name; Type type;
        boolean boolVal; double numVal, min, max, step;
        String[] modes; int modeIdx;

        static Setting toggle(String n, boolean def){ Setting s=new Setting(); s.name=n; s.type=Type.TOGGLE; s.boolVal=def; return s; }
        static Setting slider(String n, double def, double min, double max, double step){ Setting s=new Setting(); s.name=n; s.type=Type.SLIDER; s.numVal=def; s.min=min; s.max=max; s.step=step; return s; }
        static Setting mode(String n, String... modes){ Setting s=new Setting(); s.name=n; s.type=Type.MODE; s.modes=modes; s.modeIdx=0; return s; }
    }

    static class Module {
        String name, desc; Category cat; boolean on; int key;
        List<Setting> settings = new ArrayList<>();
        boolean expanded = false;
        Module(String name, String desc, Category cat, boolean on, int key, Setting... sets){
            this.name=name; this.desc=desc; this.cat=cat; this.on=on; this.key=key;
            settings.addAll(Arrays.asList(sets));
        }
    }

    // ─── Lista modulow ────────────────────────────────────────────
    List<Module> modules = new ArrayList<>(Arrays.asList(
        new Module("TargetHUD","Wskaznik celu - HP, nazwa, dystans",Category.RENDER,true,0,
            Setting.toggle("Pokaz HP",true), Setting.toggle("Pokaz dystans",true),
            Setting.mode("Styl","Rockstar","Minimal"), Setting.slider("Skala",1.0,0.8,1.5,0.1)),
        new Module("TargetESP","Kontur wokol celu pod celownikiem",Category.RENDER,true,0,
            Setting.mode("Kolor","Cyan","Zolty","Bialy","Czerwony"),
            Setting.slider("Powiekszenie",0.05,0.0,0.3,0.01)),
        new Module("Watermark","Logo klienta na ekranie",Category.RENDER,true,0,
            Setting.toggle("Pokaz FPS",true), Setting.mode("Styl","Rockstar","Minimal")),
        new Module("Coordinates","Wspolrzedne gracza X Y Z",Category.RENDER,false,0,
            Setting.toggle("Pokaz Biom",true), Setting.slider("Rozmiar",1.0,0.5,2.0,0.1)),
        new Module("Fullbright","Pelne oswietlenie w jaskiniach",Category.RENDER,false,0,
            Setting.slider("Jasnosc",10.0,1.0,15.0,0.5)),
        new Module("Sprint","Auto-sprint bez trzymania klawisza",Category.MOVEMENT,false,0,
            Setting.toggle("We wszystkich kierunkach",false)),
        new Module("Twoj Modul","Szablon - dostosuj jak chcesz",Category.MISC,false,0,
            Setting.toggle("Opcja 1",true), Setting.slider("Predkosc",5.0,1.0,10.0,0.5),
            Setting.mode("Tryb","Normalny","Szybki","Niestandardowy"))
    ));

    // ─── Stan GUI ─────────────────────────────────────────────────
    Category selectedCat = Category.RENDER;
    String searchQuery = "";
    int scrollOffset = 0;
    Module keybindingFor = null;

    // ─── Panele ───────────────────────────────────────────────────
    SidebarPanel sidebar;
    ContentPanel content;
    JTextField searchField;

    public RockstarGUI() {
        setTitle("★ Rockstar Client");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setUndecorated(true);
        setSize(700, 440);
        setLocationRelativeTo(null);
        setBackground(new Color(0,0,0,0));

        // Ciemna, zaokraglona ramka
        JPanel root = new JPanel(new BorderLayout()) {
            @Override protected void paintComponent(Graphics g){
                Graphics2D g2=(Graphics2D)g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(BG);
                g2.fillRoundRect(0,0,getWidth(),getHeight(),12,12);
                g2.dispose();
            }
        };
        root.setOpaque(false);
        root.setBorder(new EmptyBorder(1,1,1,1));

        // Naglowek
        root.add(buildHeader(), BorderLayout.NORTH);

        // Glowny obszar
        JPanel body = new JPanel(new BorderLayout());
        body.setOpaque(false);
        sidebar = new SidebarPanel();
        content = new ContentPanel();
        JScrollPane scrollPane = new JScrollPane(content);
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setOpaque(false);
        scrollPane.setBorder(null);
        scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.getVerticalScrollBar().setUI(new DarkScrollBarUI());
        scrollPane.getVerticalScrollBar().setPreferredSize(new Dimension(6,0));

        body.add(sidebar, BorderLayout.WEST);
        body.add(scrollPane, BorderLayout.CENTER);
        root.add(body, BorderLayout.CENTER);

        setContentPane(root);

        // Przeciaganie okna
        DragListener drag = new DragListener(this);
        root.addMouseListener(drag);
        root.addMouseMotionListener(drag);

        // Klawiatura (Keybind)
        setFocusTraversalKeysEnabled(false);
        addKeyListener(new KeyAdapter(){
            @Override public void keyPressed(KeyEvent e){
                if(keybindingFor != null){
                    if(e.getKeyCode()==KeyEvent.VK_ESCAPE || e.getKeyCode()==KeyEvent.VK_DELETE){
                        keybindingFor.key = 0;
                    } else {
                        keybindingFor.key = e.getKeyCode();
                    }
                    keybindingFor = null;
                    content.repaint();
                }
            }
        });
    }

    JPanel buildHeader(){
        JPanel h = new JPanel(null){
            @Override protected void paintComponent(Graphics g){
                Graphics2D g2=(Graphics2D)g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(0x0D,0x0F,0x16));
                g2.fillRect(0,0,getWidth(),getHeight());
                g2.setColor(SEP);
                g2.fillRect(0,getHeight()-1,getWidth(),1);
                // Logo
                g2.setFont(new Font("Segoe UI",Font.BOLD,15));
                g2.setColor(ACCENT);
                g2.drawString("\u2605 ROCKSTAR",16,28);
                g2.setFont(new Font("Segoe UI",Font.PLAIN,11));
                g2.setColor(TEXT_DIM);
                g2.drawString("CLIENT",104,28);
                // Wersja
                g2.setFont(new Font("Segoe UI",Font.PLAIN,11));
                g2.setColor(new Color(0x38,0x40,0x56));
                g2.drawString("v1.0 | Java Edition",160,28);
                g2.dispose();
            }
        };
        h.setOpaque(false);
        h.setPreferredSize(new Dimension(0,48));

        // Wyszukiwarka
        searchField = new JTextField(){
            @Override protected void paintComponent(Graphics g){
                Graphics2D g2=(Graphics2D)g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(hasFocus()?new Color(0x1A,0x1E,0x2B):new Color(0x14,0x17,0x22));
                g2.fillRoundRect(0,0,getWidth(),getHeight(),6,6);
                g2.setColor(hasFocus()?ACCENT:new Color(0x28,0x2E,0x42));
                g2.drawRoundRect(0,0,getWidth()-1,getHeight()-1,6,6);
                super.paintComponent(g);
                g2.dispose();
            }
        };
        searchField.setOpaque(false);
        searchField.setForeground(TEXT_MAIN);
        searchField.setCaretColor(ACCENT);
        searchField.setBorder(new EmptyBorder(4,8,4,8));
        searchField.setFont(new Font("Segoe UI",Font.PLAIN,12));
        searchField.putClientProperty("JTextField.placeholderText","  Szukaj modulu...");
        searchField.setBounds(380,12,160,26);
        searchField.addKeyListener(new KeyAdapter(){
            @Override public void keyReleased(KeyEvent e){
                searchQuery = searchField.getText().trim();
                content.repaint();
            }
        });
        h.add(searchField);

        // Przycisk zamkniecia
        JButton close = new JButton("✕"){
            @Override protected void paintComponent(Graphics g){
                Graphics2D g2=(Graphics2D)g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
                if(getModel().isRollover()) g2.setColor(new Color(0xC0,0x3A,0x3A));
                else g2.setColor(new Color(0x22,0x25,0x33));
                g2.fillRoundRect(0,0,getWidth(),getHeight(),6,6);
                g2.setFont(new Font("Segoe UI",Font.PLAIN,12));
                g2.setColor(getModel().isRollover()?Color.WHITE:TEXT_DIM);
                FontMetrics fm=g2.getFontMetrics();
                g2.drawString("✕",(getWidth()-fm.stringWidth("✕"))/2,(getHeight()+fm.getAscent()-fm.getDescent())/2);
                g2.dispose();
            }
        };
        close.setOpaque(false); close.setContentAreaFilled(false); close.setBorderPainted(false);
        close.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        close.setBounds(657,12,28,26);
        close.addActionListener(e -> System.exit(0));
        h.add(close);

        // Minimalizacja
        JButton mini = new JButton("─"){
            @Override protected void paintComponent(Graphics g){
                Graphics2D g2=(Graphics2D)g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
                if(getModel().isRollover()) g2.setColor(new Color(0x1A,0x1E,0x2B));
                else g2.setColor(new Color(0x22,0x25,0x33));
                g2.fillRoundRect(0,0,getWidth(),getHeight(),6,6);
                g2.setFont(new Font("Segoe UI",Font.PLAIN,12));
                g2.setColor(getModel().isRollover()?Color.WHITE:TEXT_DIM);
                FontMetrics fm=g2.getFontMetrics();
                g2.drawString("─",(getWidth()-fm.stringWidth("─"))/2,(getHeight()+fm.getAscent()-fm.getDescent())/2);
                g2.dispose();
            }
        };
        mini.setOpaque(false); mini.setContentAreaFilled(false); mini.setBorderPainted(false);
        mini.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        mini.setBounds(625,12,28,26);
        mini.addActionListener(e -> setState(JFrame.ICONIFIED));
        h.add(mini);

        return h;
    }

    // ─── Sidebar ──────────────────────────────────────────────────
    class SidebarPanel extends JPanel {
        int hoveredCat = -1;
        SidebarPanel(){
            setPreferredSize(new Dimension(145,0));
            setOpaque(false);
            addMouseListener(new MouseAdapter(){
                @Override public void mouseClicked(MouseEvent e){
                    int idx = catIndexAt(e.getY());
                    if(idx>=0){ selectedCat=Category.values()[idx]; scrollOffset=0; content.repaint(); repaint(); }
                }
                @Override public void mouseExited(MouseEvent e){ hoveredCat=-1; repaint(); }
            });
            addMouseMotionListener(new MouseMotionAdapter(){
                @Override public void mouseMoved(MouseEvent e){ hoveredCat=catIndexAt(e.getY()); repaint(); }
            });
        }
        int catIndexAt(int y){ int i=0; for(Category c:Category.values()){ int cy=16+i*38; if(y>=cy&&y<cy+30){return i;} i++; } return -1; }
        @Override protected void paintComponent(Graphics g){
            Graphics2D g2=(Graphics2D)g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(SIDEBAR);
            g2.fillRect(0,0,getWidth(),getHeight());
            g2.setColor(SEP);
            g2.fillRect(getWidth()-1,0,1,getHeight());

            int i=0;
            for(Category c:Category.values()){
                boolean sel=(c==selectedCat);
                boolean hov=(i==hoveredCat);
                int cy=16+i*38;
                if(sel){ g2.setColor(new Color(0x1A,0x1E,0x2B)); g2.fillRoundRect(8,cy,getWidth()-16,30,6,6);
                    g2.setColor(c.color); g2.fillRect(8,cy,3,30); }
                else if(hov){ g2.setColor(new Color(0x14,0x17,0x22)); g2.fillRoundRect(8,cy,getWidth()-16,30,6,6); }
                g2.setFont(new Font("Segoe UI",Font.BOLD,12));
                g2.setColor(sel?TEXT_MAIN:hov?TEXT_MID:TEXT_DIM);
                g2.drawString(c.name,20,cy+19);
                long cnt=modules.stream().filter(m->m.cat==c).count();
                g2.setFont(new Font("Segoe UI",Font.PLAIN,10));
                g2.setColor(new Color(0x40,0x48,0x60));
                g2.drawString(String.valueOf(cnt),getWidth()-22,cy+19);
                i++;
            }
            // Dolny info
            g2.setColor(new Color(0x10,0x13,0x1E));
            g2.fillRoundRect(8,getHeight()-44,getWidth()-16,36,6,6);
            g2.setColor(ACCENT);
            g2.fillRect(14,getHeight()-38,14,14);
            g2.setFont(new Font("Segoe UI",Font.PLAIN,11));
            g2.setColor(TEXT_MID);
            g2.drawString("Dev User",34,getHeight()-27);
            g2.dispose();
        }
    }

    // ─── Content ──────────────────────────────────────────────────
    class ContentPanel extends JPanel {
        int hoveredModIdx = -1;
        Module sliderModule = null;
        Setting sliderSetting = null;
        int sliderX=0, sliderW=0;

        ContentPanel(){
            setOpaque(false);
            setLayout(null);
            addMouseListener(new MouseAdapter(){
                @Override public void mousePressed(MouseEvent e){ handlePress(e); requestFocusInWindow(); }
                @Override public void mouseReleased(MouseEvent e){ sliderModule=null; sliderSetting=null; }
            });
            addMouseMotionListener(new MouseMotionAdapter(){
                @Override public void mouseMoved(MouseEvent e){ handleMove(e); }
                @Override public void mouseDragged(MouseEvent e){ handleDrag(e); }
            });
        }

        List<Module> visible(){
            List<Module> out=new ArrayList<>();
            for(Module m:modules){
                if(searchQuery.isEmpty()?m.cat==selectedCat:m.name.toLowerCase().contains(searchQuery.toLowerCase()))
                    out.add(m);
            }
            return out;
        }

        @Override public Dimension getPreferredSize(){
            int h=10;
            for(Module m:visible()){
                h+=cardHeight(m)+8;
            }
            return new Dimension(0,Math.max(h,50));
        }

        int cardHeight(Module m){ return m.expanded&&!m.settings.isEmpty()?48+m.settings.size()*28+4:48; }

        @Override protected void paintComponent(Graphics g){
            Graphics2D g2=(Graphics2D)g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(BG);
            g2.fillRect(0,0,getWidth(),getHeight());

            int W=getWidth()-12;
            int y=8;
            List<Module> vis=visible();
            for(int i=0;i<vis.size();i++){
                Module m=vis.get(i);
                int ch=cardHeight(m);
                boolean hov=(i==hoveredModIdx);

                // Karta tlo
                g2.setColor(m.on?CARD_ON:hov?new Color(0x16,0x19,0x24):CARD);
                g2.fillRoundRect(6,y,W,ch,8,8);
                // Obramowanie
                g2.setColor(m.on?ACCENT:CARD_BORDER);
                g2.drawRoundRect(6,y,W,ch,8,8);
                // Lewy pasek
                if(m.on){ g2.setColor(ACCENT); g2.fillRoundRect(6,y,4,ch,4,4); }

                // Nazwa
                g2.setFont(new Font("Segoe UI",Font.BOLD,13));
                g2.setColor(m.on?TEXT_MAIN:TEXT_MID);
                g2.drawString(m.name,20,y+18);

                // Opis
                g2.setFont(new Font("Segoe UI",Font.PLAIN,11));
                g2.setColor(TEXT_DIM);
                g2.drawString(m.desc,20,y+33);

                // Keybind przycisk
                String kn=keyName(m.key);
                if(keybindingFor==m) kn="...";
                int kw=g2.getFontMetrics().stringWidth(kn)+10;
                int kx=W-kw-52;
                g2.setColor(new Color(0x1A,0x1E,0x2B));
                g2.fillRoundRect(kx,y+14,kw,18,4,4);
                g2.setColor(new Color(0x30,0x37,0x4C));
                g2.drawRoundRect(kx,y+14,kw,18,4,4);
                g2.setColor(keybindingFor==m?ACCENT:new Color(0x8A,0x93,0xAA));
                g2.drawString(kn,kx+5,y+26);

                // Switch
                int sw=36,sh=18,sx=W-sw-12,sy=y+15;
                g2.setColor(m.on?ACCENT2:new Color(0x22,0x26,0x34));
                g2.fillRoundRect(sx,sy,sw,sh,sh,sh);
                g2.setColor(m.on?ACCENT:new Color(0x30,0x36,0x4A));
                g2.drawRoundRect(sx,sy,sw,sh,sh,sh);
                int knobX=m.on?(sx+sw-sh+2):(sx+2);
                g2.setColor(Color.WHITE);
                g2.fillOval(knobX,sy+2,sh-4,sh-4);

                // Strzalka rozwijania (jesli sa ustawienia)
                if(!m.settings.isEmpty()){
                    g2.setFont(new Font("Segoe UI",Font.PLAIN,10));
                    g2.setColor(new Color(0x7B,0x84,0x9C));
                    g2.drawString(m.expanded?"▲":"▼",sx-18,y+27);
                }

                // Ustawienia (jesli rozwiniete)
                if(m.expanded&&!m.settings.isEmpty()){
                    int sy2=y+50;
                    g2.setColor(new Color(0x0E,0x10,0x17));
                    g2.fillRoundRect(14,sy2-2,W-16,m.settings.size()*28,6,6);

                    for(Setting s:m.settings){
                        g2.setFont(new Font("Segoe UI",Font.PLAIN,12));
                        g2.setColor(new Color(0x9C,0xA5,0xBC));
                        g2.drawString(s.name,22,sy2+16);

                        if(s.type==Setting.Type.TOGGLE){
                            int tsw=28,tsh=14,tsx=W-40,tsy=sy2+5;
                            g2.setColor(s.boolVal?ACCENT2:new Color(0x22,0x26,0x34));
                            g2.fillRoundRect(tsx,tsy,tsw,tsh,tsh,tsh);
                            int tkx=s.boolVal?(tsx+tsw-tsh+1):(tsx+1);
                            g2.setColor(Color.WHITE);
                            g2.fillOval(tkx,tsy+1,tsh-2,tsh-2);

                        } else if(s.type==Setting.Type.SLIDER){
                            int slw=120,slx=W-170,sly=sy2+12;
                            double prog=(s.numVal-s.min)/(s.max-s.min);
                            int fill=(int)(prog*slw);
                            g2.setColor(new Color(0x1C,0x20,0x2C));
                            g2.fillRoundRect(slx,sly,slw,6,3,3);
                            g2.setColor(ACCENT2);
                            g2.fillRoundRect(slx,sly,fill,6,3,3);
                            g2.setColor(Color.WHITE);
                            g2.fillOval(slx+fill-5,sly-3,10,12);
                            g2.setFont(new Font("Segoe UI",Font.PLAIN,11));
                            g2.setColor(ACCENT);
                            String val=String.format("%.1f",s.numVal);
                            g2.drawString(val,slx+slw+8,sy2+18);

                        } else if(s.type==Setting.Type.MODE){
                            String mt="[ "+s.modes[s.modeIdx]+" ▸ ]";
                            int mw=g2.getFontMetrics().stringWidth(mt)+10;
                            int mx=W-mw-14;
                            g2.setColor(new Color(0x1A,0x1E,0x2B));
                            g2.fillRoundRect(mx,sy2+4,mw,18,4,4);
                            g2.setColor(new Color(0x2B,0x32,0x47));
                            g2.drawRoundRect(mx,sy2+4,mw,18,4,4);
                            g2.setColor(ACCENT);
                            g2.drawString(mt,mx+5,sy2+16);
                        }
                        sy2+=28;
                    }
                }
                y+=ch+8;
            }
            g2.dispose();
            setPreferredSize(new Dimension(0, y+4));
        }

        void handleMove(MouseEvent e){
            int W=getWidth()-12;
            int y=8; int prev=hoveredModIdx;
            List<Module> vis=visible();
            hoveredModIdx=-1;
            for(int i=0;i<vis.size();i++){
                Module m=vis.get(i);
                int ch=cardHeight(m);
                if(e.getY()>=y&&e.getY()<y+ch){ hoveredModIdx=i; break; }
                y+=ch+8;
            }
            if(hoveredModIdx!=prev) repaint();
        }

        void handlePress(MouseEvent e){
            int W=getWidth()-12;
            int y=8;
            List<Module> vis=visible();
            for(Module m:vis){
                int ch=cardHeight(m);
                if(e.getY()>=y&&e.getY()<y+ch){
                    int sw=36,sh=18,sx=W-sw-12,sy=y+15;
                    // Klik w przelacznik
                    Rectangle switchRect=new Rectangle(sx,sy,sw,sh);
                    if(switchRect.contains(e.getPoint())){ m.on=!m.on; repaint(); return; }
                    // Klik w rozwijanie
                    if(!m.settings.isEmpty()){
                        Rectangle expRect=new Rectangle(sx-24,y+14,20,20);
                        if(expRect.contains(e.getPoint())){ m.expanded=!m.expanded; revalidate(); repaint(); return; }
                    }
                    // Klik w keybind
                    String kn=keyName(m.key); if(keybindingFor==m) kn="...";
                    FontMetrics fm=getFontMetrics(new Font("Segoe UI",Font.PLAIN,11));
                    int kw=fm.stringWidth(kn)+10,kx=W-kw-52;
                    Rectangle keyRect=new Rectangle(kx,y+14,kw,18);
                    if(keyRect.contains(e.getPoint())){ keybindingFor=(keybindingFor==m)?null:m; repaint(); return; }
                    // Prawy przycisk = rozwin
                    if(e.getButton()==MouseEvent.BUTTON3&&!m.settings.isEmpty()){ m.expanded=!m.expanded; revalidate(); repaint(); return; }
                    // Klik w ustawienia
                    if(m.expanded&&!m.settings.isEmpty()){
                        int sy2=y+50;
                        for(Setting s:m.settings){
                            if(e.getY()>=sy2&&e.getY()<sy2+28){
                                if(s.type==Setting.Type.TOGGLE){ s.boolVal=!s.boolVal; repaint(); return; }
                                if(s.type==Setting.Type.MODE){ s.modeIdx=(s.modeIdx+1)%s.modes.length; repaint(); return; }
                                if(s.type==Setting.Type.SLIDER){
                                    int slx=W-170; sliderModule=m; sliderSetting=s;
                                    sliderX=slx; sliderW=120;
                                    updateSlider(e.getX());
                                    return;
                                }
                            }
                            sy2+=28;
                        }
                    }
                    break;
                }
                y+=ch+8;
            }
        }

        void handleDrag(MouseEvent e){
            if(sliderSetting!=null){ updateSlider(e.getX()); }
        }

        void updateSlider(int mx){
            if(sliderSetting==null) return;
            double p=Math.max(0,Math.min(1,(double)(mx-sliderX)/sliderW));
            double raw=sliderSetting.min+p*(sliderSetting.max-sliderSetting.min);
            if(sliderSetting.step>0){
                raw=Math.round((raw-sliderSetting.min)/sliderSetting.step)*sliderSetting.step+sliderSetting.min;
            }
            sliderSetting.numVal=Math.max(sliderSetting.min,Math.min(sliderSetting.max,raw));
            repaint();
        }
    }

    // ─── Pomocnicze ───────────────────────────────────────────────
    static String keyName(int k){ if(k<=0) return "NONE"; return KeyEvent.getKeyText(k).toUpperCase(); }

    static class DragListener extends MouseAdapter {
        JFrame f; Point start;
        DragListener(JFrame f){this.f=f;}
        @Override public void mousePressed(MouseEvent e){ start=e.getPoint(); }
        @Override public void mouseDragged(MouseEvent e){
            if(start!=null){ Point l=f.getLocation(); f.setLocation(l.x+e.getX()-start.x,l.y+e.getY()-start.y); }
        }
    }

    static class DarkScrollBarUI extends javax.swing.plaf.basic.BasicScrollBarUI {
        @Override protected void configureScrollBarColors(){ thumbColor=new Color(0x00,0xC0,0xFA); trackColor=new Color(0x16,0x19,0x24); }
        @Override protected JButton createDecreaseButton(int o){ return createZeroButton(); }
        @Override protected JButton createIncreaseButton(int o){ return createZeroButton(); }
        JButton createZeroButton(){ JButton b=new JButton(); b.setPreferredSize(new Dimension(0,0)); return b; }
        @Override protected void paintThumb(Graphics g, JComponent c, Rectangle r){
            Graphics2D g2=(Graphics2D)g; g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(thumbColor); g2.fillRoundRect(r.x,r.y,r.width,r.height,4,4);
        }
        @Override protected void paintTrack(Graphics g, JComponent c, Rectangle r){
            g.setColor(trackColor); g.fillRect(r.x,r.y,r.width,r.height);
        }
    }

    public static void main(String[] args) throws Exception {
        System.setProperty("awt.useSystemAAFontSettings","on");
        System.setProperty("swing.aatext","true");
        UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        SwingUtilities.invokeLater(()->{
            RockstarGUI gui=new RockstarGUI();
            gui.setVisible(true);
        });
    }
}
