/*
 * (c) 2008-2009 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Audio-Ausgang
 * Emulation des Anschlusses des Magnettonbandgeraetes
 */

package jkcemu.audio;

import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.lang.*;
import java.util.*;
import javax.sound.sampled.*;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import jkcemu.Main;
import jkcemu.base.*;
import z80emu.*;


public class AudioFrm
		extends BasicFrm
		implements ChangeListener, Z80MaxSpeedListener
{
  private static final int[] sampleRates = {
				96000, 48000, 44100, 32000,
				22050, 16000, 11025, 8000 };

  private ScreenFrm         screenFrm;
  private EmuThread         emuThread;
  private Z80CPU            z80cpu;
  private Mixer.Info[]      mixers;
  private AudioFormat       audioFmt;
  private AudioIO           audioIO;
  private FloatControl      controlVolume;
  private File              curFile;
  private File              lastFile;
  private boolean           lastIsTAP;
  private int               lastVolumeSliderValue;
  private boolean           blinkState;
  private Color             blinkColor0;
  private Color             blinkColor1;
  private javax.swing.Timer blinkTimer;
  private JRadioButton      btnSoundOut;
  private JRadioButton      btnDataOut;
  private JRadioButton      btnDataIn;
  private JRadioButton      btnSoundFileOut;
  private JRadioButton      btnSoundFileIn;
  private JRadioButton      btnTAPFileIn;
  private JRadioButton      btnFileLastIn;
  private JLabel            labelMixer;
  private JComboBox         comboMixer;
  private JLabel            labelSampleRate;
  private JComboBox         comboSampleRate;
  private JLabel            labelChannel;
  private JRadioButton      btnChannel0;
  private JRadioButton      btnChannel1;
  private JCheckBox         btnMonitor;
  private JLabel            labelFileName;
  private JTextField        fldFileName;
  private JLabel            labelFormat;
  private JTextField        fldFormat;
  private JLabel            labelProgress;
  private JProgressBar      progressBar;
  private JPanel            panelVolume;
  private JSlider           sliderVolume;
  private JButton           btnEnable;
  private JButton           btnDisable;
  private JButton           btnHelp;
  private JButton           btnClose;
  private JButton           btnPlay;
  private JButton           btnPause;


  public AudioFrm( ScreenFrm screenFrm )
  {
    this.screenFrm             = screenFrm;
    this.emuThread             = screenFrm.getEmuThread();
    this.z80cpu                = this.emuThread.getZ80CPU();
    this.mixers                = AudioSystem.getMixerInfo();
    this.audioFmt              = null;
    this.audioIO               = null;
    this.controlVolume         = null;
    this.curFile               = null;
    this.lastFile              = null;
    this.lastIsTAP             = false;

    setTitle( "JKCEMU Audio/Kassette" );
    Main.updIcon( this );


    // Fensterinhalt
    setLayout( new GridBagLayout() );

    GridBagConstraints gbc = new GridBagConstraints(
						0, 0,
						1, 1,
						1.0, 0.0,
						GridBagConstraints.NORTHWEST,
						GridBagConstraints.HORIZONTAL,
						new Insets( 5, 5, 5, 5 ),
						0, 0 );


    // Bereich Funktion
    JPanel panelFct = new JPanel( new GridBagLayout() );
    panelFct.setBorder( BorderFactory.createTitledBorder( "Funktion" ) );

    GridBagConstraints gbcFct = new GridBagConstraints(
						0, 0,
						1, 1,
						0.0, 0.0,
						GridBagConstraints.NORTHWEST,
						GridBagConstraints.NONE,
						new Insets( 5, 5, 0, 5 ),
						0, 0 );

    ButtonGroup grpFct = new ButtonGroup();

    this.btnSoundOut = new JRadioButton(
			"T\u00F6ne ausgeben (Emulation des Lautsprechers)",
			true );
    grpFct.add( this.btnSoundOut );
    this.btnSoundOut.addActionListener( this );
    panelFct.add( this.btnSoundOut, gbcFct );

    this.btnDataOut = new JRadioButton( "Daten am Audio-Ausgang ausgeben" );
    grpFct.add( this.btnDataOut );
    this.btnDataOut.addActionListener( this );
    gbcFct.insets.top = 0;
    gbcFct.gridy++;
    panelFct.add( this.btnDataOut, gbcFct );

    this.btnDataIn = new JRadioButton( "Daten vom Audio-Eingang lesen" );
    grpFct.add( this.btnDataIn );
    this.btnDataIn.addActionListener( this );
    gbcFct.gridy++;
    panelFct.add( this.btnDataIn, gbcFct );

    this.btnSoundFileOut = new JRadioButton( "Sound-Datei speichern" );
    grpFct.add( this.btnSoundFileOut );
    this.btnSoundFileOut.addActionListener( this );
    gbcFct.gridy++;
    panelFct.add( this.btnSoundFileOut, gbcFct );

    this.btnSoundFileIn = new JRadioButton( "Sound-Datei lesen" );
    grpFct.add( this.btnSoundFileIn );
    this.btnSoundFileIn.addActionListener( this );
    gbcFct.gridy++;
    panelFct.add( this.btnSoundFileIn, gbcFct );

    this.btnTAPFileIn = new JRadioButton( "KC-TAP-Datei lesen" );
    grpFct.add( this.btnTAPFileIn );
    this.btnTAPFileIn.addActionListener( this );
    gbcFct.gridy++;
    panelFct.add( this.btnTAPFileIn, gbcFct );

    this.btnFileLastIn = new JRadioButton(
			"Letzte Sound-/TAP-Datei (noch einmal) lesen" );
    grpFct.add( this.btnFileLastIn );
    this.btnFileLastIn.addActionListener( this );
    gbcFct.insets.bottom = 5;
    gbcFct.gridy++;
    panelFct.add( this.btnFileLastIn, gbcFct );

    add( panelFct, gbc );


    // Bereich Optionen
    JPanel panelOpt = new JPanel( new GridBagLayout() );
    panelOpt.setBorder( BorderFactory.createTitledBorder( "Optionen" ) );

    GridBagConstraints gbcOpt = new GridBagConstraints(
						0, 0,
						1, 1,
						0.0, 0.0,
						GridBagConstraints.WEST,
						GridBagConstraints.NONE,
						new Insets( 5, 5, 2, 5 ),
						0, 0 );

    this.labelMixer = new JLabel( "Ger\u00E4t:" );
    panelOpt.add( this.labelMixer, gbcOpt );

    this.comboMixer = new JComboBox();
    this.comboMixer.setEditable( false );
    this.comboMixer.addItem( "Standard" );
    if( this.mixers != null ) {
      for( int i = 0; i < this.mixers.length; i++ ) {
	String s = this.mixers[ i ].getName();
	if( s != null ) {
	  if( s.isEmpty() ) {
	    s = null;
	  }
	}
	this.comboMixer.addItem( s != null ? s : "unbekannt" );
      }
    }
    gbcOpt.gridwidth = GridBagConstraints.REMAINDER;
    gbcOpt.gridx++;
    panelOpt.add( this.comboMixer, gbcOpt );

    this.labelSampleRate = new JLabel( "Abtastrate (Hz):" );
    gbcOpt.insets.top = 2;
    gbcOpt.gridwidth  = 1;
    gbcOpt.gridx      = 0;
    gbcOpt.gridy++;
    panelOpt.add( this.labelSampleRate, gbcOpt );

    this.comboSampleRate = new JComboBox();
    this.comboSampleRate.setEditable( false );
    this.comboSampleRate.addItem( "Standard" );
    for( int i = 0; i < this.sampleRates.length; i++ ) {
      this.comboSampleRate.addItem( String.valueOf( this.sampleRates[ i ] ) );
    }
    gbcOpt.gridwidth = GridBagConstraints.REMAINDER;
    gbcOpt.gridx++;
    panelOpt.add( this.comboSampleRate, gbcOpt );

    this.labelChannel = new JLabel( "Aktiver Kanal:" );
    gbcOpt.insets.top = 2;
    gbcOpt.gridwidth  = 1;
    gbcOpt.gridx      = 0;
    gbcOpt.gridy++;
    panelOpt.add( this.labelChannel, gbcOpt );

    ButtonGroup grpChannel = new ButtonGroup();

    this.btnChannel0 = new JRadioButton( "Links", true );
    grpChannel.add( this.btnChannel0 );
    this.btnChannel0.addActionListener( this );
    gbcOpt.gridx++;
    panelOpt.add( this.btnChannel0, gbcOpt );

    this.btnChannel1 = new JRadioButton( "Rechts", false );
    grpChannel.add( this.btnChannel1 );
    this.btnChannel1.addActionListener( this );
    gbcOpt.gridx++;
    panelOpt.add( this.btnChannel1, gbcOpt );

    this.btnMonitor = new JCheckBox( "Mith\u00F6ren", false );
    this.btnMonitor.addActionListener( this );
    gbcOpt.insets.bottom = 5;
    gbcOpt.gridwidth     = GridBagConstraints.REMAINDER;
    gbcOpt.gridx         = 0;
    gbcOpt.gridy++;
    panelOpt.add( this.btnMonitor, gbcOpt );

    gbc.gridy++;
    add( panelOpt, gbc );


    // Bereich Status
    JPanel panelStatus = new JPanel( new GridBagLayout() );
    panelStatus.setBorder( BorderFactory.createTitledBorder( "Status" ) );

    GridBagConstraints gbcStatus = new GridBagConstraints(
						0, 0,
						1, 1,
						0.0, 0.0,
						GridBagConstraints.EAST,
						GridBagConstraints.NONE,
						new Insets( 5, 5, 5, 5 ),
						0, 0 );

    this.labelFileName = new JLabel( "Datei:" );
    panelStatus.add( this.labelFileName, gbcStatus );

    this.labelFormat = new JLabel( "Format:" );
    gbcStatus.gridy++;
    panelStatus.add( this.labelFormat, gbcStatus );

    this.labelProgress = new JLabel( "Fortschritt:" );
    gbcStatus.gridy++;
    panelStatus.add( this.labelProgress, gbcStatus );

    this.fldFileName = new JTextField();
    this.fldFileName.setEditable( false );
    gbcStatus.anchor  = GridBagConstraints.WEST;
    gbcStatus.fill    = GridBagConstraints.HORIZONTAL;
    gbcStatus.weightx = 1.0;
    gbcStatus.gridy   = 0;
    gbcStatus.gridx++;
    panelStatus.add( this.fldFileName, gbcStatus );

    this.fldFormat = new JTextField();
    this.fldFormat.setEditable( false );
    gbcStatus.gridy++;
    panelStatus.add( this.fldFormat, gbcStatus );

    this.progressBar = new JProgressBar( JProgressBar.HORIZONTAL, 0, 100 );
    this.progressBar.setBorderPainted( true );
    this.progressBar.setStringPainted( false );
    this.progressBar.setValue( 0 );
    gbcStatus.gridy++;
    panelStatus.add( this.progressBar, gbcStatus );

    gbc.gridy++;
    add( panelStatus, gbc );


    // linker Bereich
    JPanel panelLeft = new JPanel( new GridBagLayout() );
    gbc.fill       = GridBagConstraints.VERTICAL;
    gbc.weighty    = 1.0;
    gbc.gridheight = GridBagConstraints.REMAINDER;
    gbc.gridy      = 0;
    gbc.gridx++;
    add( panelLeft, gbc );

    GridBagConstraints gbcLeft = new GridBagConstraints(
						0, 0,
						1, 1,
						0.0, 0.0,
						GridBagConstraints.CENTER,
						GridBagConstraints.HORIZONTAL,
						new Insets( 5, 5, 0, 5 ),
						0, 0 );

    this.btnEnable = new JButton( "Aktivieren" );
    this.btnEnable.addActionListener( this );
    panelLeft.add( this.btnEnable, gbcLeft );

    this.btnDisable = new JButton( "Deaktivieren" );
    this.btnDisable.addActionListener( this );
    gbcLeft.gridy++;
    panelLeft.add( this.btnDisable, gbcLeft );

    this.btnPlay = new JButton( "Abspielen" );
    this.btnPlay.addActionListener( this );
    gbcLeft.gridy++;
    panelLeft.add( this.btnPlay, gbcLeft );

    this.btnPause = new JButton( "Pause" );
    this.btnPause.addActionListener( this );
    gbcLeft.gridy++;
    panelLeft.add( this.btnPause, gbcLeft );

    this.btnHelp = new JButton( "Hilfe" );
    this.btnHelp.addActionListener( this );
    gbcLeft.gridy++;
    panelLeft.add( this.btnHelp, gbcLeft );

    this.btnClose = new JButton( "Schlie\u00DFen" );
    this.btnClose.addActionListener( this );
    gbcLeft.gridy++;
    panelLeft.add( this.btnClose, gbcLeft );


    // Lautstaerke
    this.panelVolume = new JPanel( new GridBagLayout() );
    this.panelVolume.setBorder( BorderFactory.createTitledBorder(
						"Lautst\u00E4rke" ) );
    gbcLeft.fill       = GridBagConstraints.BOTH;
    gbcLeft.weighty    = 1.0;
    gbcLeft.insets.top = 30;
    gbcLeft.gridy++;
    panelLeft.add( panelVolume, gbcLeft );

    GridBagConstraints gbcVolume = new GridBagConstraints(
						0, 0,
						1, 1,
						0.0, 0.0,
						GridBagConstraints.CENTER,
						GridBagConstraints.VERTICAL,
						new Insets( 5, 5, 5, 5 ),
						0, 0 );

    this.sliderVolume = new JSlider( SwingConstants.VERTICAL, 0, 100, 0 );
    this.sliderVolume.addChangeListener( this );
    this.panelVolume.add( this.sliderVolume, gbcVolume );

    // Blinken
    this.blinkState  = false;
    this.blinkColor0 = this.btnPlay.getForeground();
    this.blinkColor1 = new Color( 0, 180, 0 );
    this.blinkTimer  = new javax.swing.Timer(
			800,
			new ActionListener()
			{
			  public void actionPerformed( ActionEvent e )
			  {
			    changeBlinkState();
			  }
			} );

    // Initialzustand
    setAudioState( false, false );
    this.lastVolumeSliderValue = -1;	// nach setAudioState(...) !!!

    // sonstiges
    pack();
    if( !applySettings( Main.getProperties(), false ) ) {
      setScreenCentered();
    }
    setResizable( false );
    this.z80cpu.addMaxSpeedListener( this );
  }


  public void fireDisable()
  {
    SwingUtilities.invokeLater(
		new Runnable()
		{
		  public void run()
		  {
		    doDisable();
		  }
		} );
  }


  public void fireProgressUpdate( final float value )
  {
    SwingUtilities.invokeLater(
		new Runnable()
		{
		  public void run()
		  {
		    updProgressBar( value );
		  }
		} );
  }


  public void doQuit()
  {
    stopBlinking();
    doDisable();
    doClose();
  }


  public void openTAP( File file, byte[] fileBytes, int offs )
  {
    int speedKHz = getSpeedKHz();
    if( speedKHz > 0 )
      enableAudioInFile( speedKHz, file, fileBytes, offs, true );
  }


	/* --- ChangeListener --- */

  public void stateChanged( ChangeEvent e )
  {
    Object src = e.getSource();
    if( src != null ) {
      if( (src == this.sliderVolume) && (this.controlVolume != null) ) {
	int vMinSlider  = this.sliderVolume.getMinimum();
	int vMaxSlider  = this.sliderVolume.getMaximum();
	int rangeSlider = vMaxSlider - vMinSlider;
	if( rangeSlider > 0 ) {
	  float vNorm = (float) (this.sliderVolume.getValue() - vMinSlider)
							/ (float) rangeSlider;
	  if( vNorm < 0F ) {
	    vNorm = 0F;
	  } else if( vNorm > 1F ) {
	    vNorm = 1F;
	  }
	  float vMinCtrl = this.controlVolume.getMinimum();
	  float vMaxCtrl = this.controlVolume.getMaximum();
	  float vCtrl    = vMinCtrl + (vNorm * (vMaxCtrl - vMinCtrl));
	  if( vCtrl < vMinCtrl ) {
	    vCtrl = vMinCtrl;
	  } else if( vCtrl > vMaxCtrl ) {
	    vCtrl = vMaxCtrl;
	  }
	  this.controlVolume.setValue( vCtrl );
	}
      }
    }
  }


	/* --- Z80MaxSpeedListener --- */

  /*
   * Wenn sich die Emulationsgeschwindigkeit aendert,
   * stimmt die Synchronisation mit dem Audio-System nicht mehr.
   * Aus diesem Grund die Audiokanaele schliessen und
   * bei Ein-/Ausgabe ueber das Sound-System wieder oeffnen.
   */
  public void z80MaxSpeedChanged( Z80CPU cpu )
  {
    SwingUtilities.invokeLater(
		new Runnable()
		{
		  public void run()
		  {
		    maxSpeedChanged();
		  }
		} );
  }


	/* --- ueberschriebene Methoden --- */

  protected boolean doAction( EventObject e )
  {
    boolean rv = false;
    stopBlinking();
    if( e != null ) {
      Object src = e.getSource();
      if( (src == this.btnSoundOut)
	  || (src == this.btnDataOut)
	  || (src == this.btnDataIn)
	  || (src == this.btnSoundFileOut)
	  || (src == this.btnSoundFileIn)
	  || (src == this.btnTAPFileIn)
	  || (src == this.btnFileLastIn) )
      {
	rv = true;
	updOptFields( this.btnTAPFileIn.isSelected()
		|| (this.btnFileLastIn.isSelected() && this.lastIsTAP) );
      }
      else if( (src == this.btnChannel0) || (src == this.btnChannel1) ) {
	rv = true;
	updChannel();
      }
      else if( src == this.btnMonitor ) {
	rv = true;
	updMonitorEnabled();
      }
      else if( src == this.btnEnable ) {
	rv = true;
	doEnable();
      }
      else if( src == this.btnDisable ) {
	rv = true;
	doDisable();
      }
      else if( src == this.btnPlay ) {
	rv = true;
	doPlay();
      }
      else if( src == this.btnPause ) {
	rv = true;
	doPause();
      }
      else if( src == this.btnHelp ) {
	rv = true;
	this.screenFrm.showHelp( "/help/audio.htm" );
      }
      else if( src == this.btnClose ) {
	rv = true;
	doClose();
      }
    }
    return rv;
  }


	/* --- private Methoden --- */

  private void changeBlinkState()
  {
    if( this.btnPlay.isEnabled()
	&& (this.blinkColor0 != null)
	&& (this.blinkColor1 != null) )
    {
      if( this.blinkState ) {
	this.btnPlay.setForeground( this.blinkColor0 );
      } else {
	this.btnPlay.setForeground( this.blinkColor1 );
      }
      this.blinkState = !this.blinkState;
    }
  }


  private void doEnable()
  {
    int speedKHz = getSpeedKHz();
    if( speedKHz > 0 ) {
      if( this.btnSoundOut.isSelected() || this.btnDataOut.isSelected() ) {
	doEnableAudioOutLine( speedKHz, this.btnDataOut.isSelected() );
      }
      else if( this.btnDataIn.isSelected() ) {
	doEnableAudioInLine( speedKHz );
      }
      else if( this.btnSoundFileOut.isSelected() ) {
	doEnableAudioOutFile( speedKHz );
      }
      else if( this.btnSoundFileIn.isSelected() ) {
	doEnableAudioInFile( speedKHz, null, false );
      }
      else if( this.btnTAPFileIn.isSelected() ) {
	doEnableAudioInFile( speedKHz, null, true );
      }
      else if( this.btnFileLastIn.isSelected() ) {
	doEnableAudioInFile( speedKHz, this.lastFile, this.lastIsTAP );
      }
    }
  }


  private void doEnableAudioInFile( int speedKHz, File file, boolean tap )
  {
    if( file == null ) {
      if( tap ) {
	file = EmuUtil.showFileOpenDlg(
				this,
				"KC-TAP-Datei \u00F6ffnen",
				Main.getLastPathFile( "software" ),
				EmuUtil.getTapFileFilter() );
      } else {
	file = EmuUtil.showFileOpenDlg(
				this,
				"Sound-Datei \u00F6ffnen",
				Main.getLastPathFile( "audio" ),
				AudioUtil.getAudioInFileFilter() );
      }
    }
    if( file != null ) {
      enableAudioInFile( speedKHz, file, null, 0, tap );
    }
  }


  private void doEnableAudioInLine( int speedKHz )
  {
    stopAudio();
    AudioIn audioIn = new AudioInLine( this.z80cpu );
    this.audioFmt   = audioIn.startAudio(
				getSelectedMixer(),
				speedKHz,
				getSampleRate() );
    if( this.audioFmt != null ) {
      this.audioIO = audioIn;
      updChannel();
      this.emuThread.setAudioIn( audioIn );
      setAudioState( true, false );
    } else {
      audioIn.stopAudio();
      showError( audioIn.getErrorText() );
    }
  }


  private void doEnableAudioOutFile( int speedKHz )
  {
    File file = EmuUtil.showFileSaveDlg(
				this,
				"Sound-Datei speichern",
				Main.getLastPathFile( "audio" ),
				AudioUtil.getAudioOutFileFilter() );
    if( file != null ) {
      AudioFileFormat.Type fileType = AudioUtil.getAudioFileType( this, file );
      if( fileType != null ) {
	stopAudio();
	AudioOut audioOut = new AudioOutFile(
					this.z80cpu,
					this,
					file,
					fileType );
	this.audioFmt = audioOut.startAudio( null, speedKHz, getSampleRate() );
	if( this.audioFmt != null ) {
	  this.audioIO   = audioOut;
	  this.curFile   = file;
	  this.lastFile  = file;
	  this.lastIsTAP = false;
	  this.emuThread.setAudioOut( audioOut );
	  Main.setLastFile( file, "audio" );
	  setAudioState( true, false );
	  updMonitorEnabled();
	} else {
	  audioOut.stopAudio();
	  showError( audioOut.getErrorText() );
	}
      }
    }
  }


  private void doEnableAudioOutLine( int speedKHz, boolean forDataTransfer )
  {
    stopAudio();
    AudioOut audioOut = new AudioOutLine(
				this.z80cpu,
				this.btnSoundOut.isSelected() );
    this.audioFmt = audioOut.startAudio(
				getSelectedMixer(),
				speedKHz,
				getSampleRate() );
    if( this.audioFmt != null ) {
      this.audioIO = audioOut;
      this.emuThread.setAudioOut( audioOut );
      setAudioState( true, false );
    } else {
      audioOut.stopAudio();
      showError( audioOut.getErrorText() );
    }
  }


  private void doDisable()
  {
    stopAudio();
    this.curFile  = null;
    this.audioFmt = null;
    setAudioState( false, false );
  }


  private void doPlay()
  {
    AudioIO audioIO = this.audioIO;
    if( audioIO != null ) {
      if( audioIO instanceof AudioInFile ) {
	((AudioInFile) audioIO).setPause( false );
	this.btnPlay.setEnabled( false );
	this.btnPause.setEnabled( true );
      }
    }
  }


  private void doPause()
  {
    AudioIO audioIO = this.audioIO;
    if( audioIO != null ) {
      if( audioIO instanceof AudioInFile ) {
	((AudioInFile) audioIO).setPause( true );
	this.btnPlay.setEnabled( true );
	this.btnPause.setEnabled( false );
      }
    }
  }


  private void enableAudioInFile(
			int     speedKHz,
			File    file,
			byte[]  fileBytes,
			int     offs,
			boolean tap )
  {
    stopAudio();
    AudioIn audioIn = new AudioInFile(
				this.z80cpu,
				this,
				file,
				fileBytes,
				offs,
				tap );
    this.audioFmt = audioIn.startAudio( null, speedKHz, getSampleRate() );
    this.audioIO  = audioIn;
    if( this.audioFmt != null ) {
      updChannel();
      this.emuThread.setAudioIn( audioIn );
      this.curFile   = file;
      this.lastFile  = file;
      this.lastIsTAP = tap;
      Main.setLastFile( file, tap ? "software" : "audio" );
      setAudioState( true, tap );
      updMonitorEnabled();
      this.btnPlay.requestFocus();
      this.blinkTimer.restart();
    } else {
      showError( this.audioIO.getErrorText() );
    }
  }


  /*
   * Die Methode liefert den Wert im Bereich von 0.0 bis 1.0
   */
  private static float getNormalizedValue( FloatControl ctrl )
  {
    float rv = 0F;
    if( ctrl != null ) {
      float range = ctrl.getMaximum() - ctrl.getMinimum();
      if( range > 0F ) {
	rv = (ctrl.getValue() - ctrl.getMinimum()) / range;
	if( rv < 0F ) {
	  rv = 0F;
	} else if( rv > 1F ) {
	  rv = 1F;
	}
      }
    }
    return rv;
  }


  private int getSampleRate()
  {
    int i = this.comboSampleRate.getSelectedIndex() - 1;  // 0: automatisch
    return ((i >= 0) && (i < this.sampleRates.length)) ?
					this.sampleRates[ i ] : 0;
  }


  private Mixer getSelectedMixer()
  {
    Mixer mixer = null;
    if( this.mixers != null ) {
      int idx = this.comboMixer.getSelectedIndex() - 1;
      if( (idx >= 0) && (idx < this.mixers.length) ) {
	try {
	  mixer = AudioSystem.getMixer( (Mixer.Info) this.mixers[ idx ] );
	}
	catch( IllegalArgumentException ex ) {}
      }
    }
    return mixer;
  }


  private int getSpeedKHz()
  {
    int speedKHz = this.z80cpu.getMaxSpeedKHz();
    if( speedKHz <= 0 ) {
      BasicDlg.showErrorDlg(
		this,
		"Sie m\u00Fcssen die Geschwindigkeit des Emulators\n"
			+ "auf einen konkreten Wert begrenzen, da dieser\n"
			+ "als Zeitbasis f\u00FCr das AudioSystem dient.\n" );
    }
    return speedKHz;
  }


  private void maxSpeedChanged()
  {
    boolean state = (this.audioFmt != null);
    doDisable();
    if( state ) {
      int speedKHz = this.z80cpu.getMaxSpeedKHz();
      if( speedKHz > 0 ) {
	if( this.btnSoundOut.isSelected() || this.btnDataOut.isSelected() ) {
	  doEnableAudioOutLine( speedKHz, this.btnDataOut.isSelected() );
	}
	else if( this.btnDataIn.isSelected() ) {
	  doEnableAudioInLine( speedKHz );
	}
      }
    }
  }


  private void setAudioState( boolean state, boolean tap )
  {
    if( state && (this.audioFmt != null) ) {
      this.labelFormat.setEnabled( true );
      if( tap ) {
	this.fldFormat.setText( "KC-TAP-Datei" );
      } else {
	this.fldFormat.setText(
		AudioUtil.getAudioFormatText( this.audioFmt ) );
      }
    } else {
      this.labelFormat.setEnabled( false );
      this.fldFormat.setText( "" );
    }

    if( state && (this.curFile != null) ) {
      this.labelFileName.setEnabled( true );
      this.fldFileName.setText( this.curFile.getPath() );
    } else {
      this.labelFileName.setEnabled( false );
      this.fldFileName.setText( "" );
    }

    boolean progressState = false;
    if( state ) {
      AudioIO audioIO = this.audioIO;
      if( audioIO != null ) {
	progressState = audioIO.isProgressUpdateEnabled();
      }
    }
    if( progressState ) {
      this.labelProgress.setEnabled( true );
      this.progressBar.setEnabled( true );
    } else {
      this.progressBar.setValue( this.progressBar.getMinimum() );
      this.progressBar.setEnabled( false );
      this.labelProgress.setEnabled( false );
    }

    this.btnDisable.setEnabled( state );

    boolean fileIn = false;
    boolean pause  = false;
    if( state ) {
      AudioIO audioIO = this.audioIO;
      if( audioIO != null ) {
	if( audioIO instanceof AudioInFile ) {
	  fileIn = true;
	  pause  = ((AudioInFile) audioIO).isPause();
	}
      }
    }
    this.btnPlay.setEnabled( fileIn && pause );
    this.btnPause.setEnabled( fileIn && !pause );
    updVolumeSliderEnabled( state );

    state = !state;
    this.btnEnable.setEnabled( state );
    this.btnEnable.setEnabled( state );
    this.btnSoundOut.setEnabled( state );
    this.btnDataOut.setEnabled( state );
    this.btnDataIn.setEnabled( state );
    this.btnSoundFileOut.setEnabled( state );
    this.btnSoundFileIn.setEnabled( state );
    this.btnTAPFileIn.setEnabled( state );
    this.btnFileLastIn.setEnabled( state && (this.lastFile != null) );
    updOptFields( tap );
  }


  private void showError( String errorText )
  {
    if( errorText == null ) {
      if( this.comboSampleRate.getSelectedIndex() > 0 ) {
	errorText = "Es konnte kein Audiokanal mit den angegebenen"
				+ " Optionen ge\u00F6ffnet werden.";
      } else {
	errorText = "Es konnte kein Audiokanal ge\u00F6ffnet werden.";
      }
    }
    BasicDlg.showErrorDlg( this, errorText );
  }


  private void stopAudio()
  {
    AudioIO audioIO = this.audioIO;
    this.audioIO    = null;
    this.emuThread.setAudioIn( null );
    this.emuThread.setAudioOut( null );
    if( audioIO != null ) {
      audioIO.stopAudio();
      String errorText = audioIO.getErrorText();
      if( errorText != null )
	BasicDlg.showErrorDlg( this, errorText );
    }
  }


  public void stopBlinking()
  {
    if( this.blinkTimer.isRunning() ) {
      this.blinkTimer.stop();
      if( this.blinkState && (this.blinkColor0 != null) ) {
	this.btnPlay.setForeground( this.blinkColor0 );
	this.blinkState = false;
      }
    }
  }


  private void updChannel()
  {
    AudioIO     audioIO  = this.audioIO;
    AudioFormat audioFmt = this.audioFmt;
    if( (audioIO != null) && (audioFmt != null) ) {
      if( audioIO instanceof AudioIn ) {
	int channel = 0;
	if( this.btnChannel1.isSelected() ) {
	  channel = 1;
	}
	if( channel >= audioFmt.getChannels() ) {
	  channel = 0;
	  this.btnChannel0.setSelected( true );
	}
	((AudioIn) audioIO).setSelectedChannel( channel );
      }
    }
  }


  private void updMonitorEnabled()
  {
    boolean curState = false;
    boolean btnState = this.btnMonitor.isSelected();
    AudioIO audioIO  = this.audioIO;
    if( audioIO != null ) {
      boolean err = false;
      curState    = audioIO.isMonitorActive();
      if( curState != btnState ) {
	if( btnState ) {
	  audioIO.openMonitorLine();
	  curState = audioIO.isMonitorActive();
	  if( !curState ) {
	    err = false;
	  }
	} else {
	  audioIO.closeMonitorLine();
	}
	updVolumeSliderEnabled( !err && btnState );
      } else {
	curState = audioIO.isMonitorActive();
      }
      if( !curState && (curState != btnState) ) {
	this.btnMonitor.setSelected( false );
      }
      if( err ) {
	BasicDlg.showErrorDlg(
		this,
		"Das Mith\u00F6ren ist nicht m\u00F6glich,\n"
			+ "da das \u00D6ffnen eines Audiokanals"
			+ " mit dem Format\n"
			+ "der Sound-Datei fehlgeschlagen ist." );
      }
    }
  }


  private void updOptFields( boolean tap )
  {
    AudioIO     audioIO  = this.audioIO;
    AudioFormat audioFmt = this.audioFmt;

    // Mixer
    boolean state = ((audioIO == null)
	&& (this.btnSoundOut.isSelected()
	    || this.btnDataOut.isSelected()
	    || this.btnDataIn.isSelected()));
    this.labelMixer.setEnabled( state );
    this.comboMixer.setEnabled( state );

    // Sample-Rate
    state = ((audioIO == null)
	&& (this.btnSoundOut.isSelected()
	    || this.btnDataOut.isSelected()
	    || this.btnDataIn.isSelected()
	    || this.btnSoundFileOut.isSelected()));
    this.labelSampleRate.setEnabled( state );
    this.comboSampleRate.setEnabled( state );

    // Kanalauswahl
    state = false;
    if( (audioIO != null) && (audioFmt != null) ) {
      if( audioIO instanceof AudioIn ) {
	if( audioFmt.getChannels() > 1 )
	  state = true;
      }
    } else {
      state = (this.btnDataIn.isSelected()
	       || this.btnSoundFileIn.isSelected()
	       || (this.btnFileLastIn.isSelected() && !tap));
    }
    this.labelChannel.setEnabled( state );
    this.btnChannel0.setEnabled( state );
    this.btnChannel1.setEnabled( state );

    // Mithoeren
    state = (this.btnSoundFileOut.isSelected()
	    || this.btnSoundFileIn.isSelected()
	    || this.btnTAPFileIn.isSelected()
	    || this.btnFileLastIn.isSelected());
    this.btnMonitor.setEnabled( state );
  }


  private void updProgressBar( float value )
  {
    int intVal = Math.round(
	value * (float) this.progressBar.getMaximum() ) + 1;

    if( intVal < this.progressBar.getMinimum() ) {
      intVal = this.progressBar.getMinimum();
    }
    else if( intVal > this.progressBar.getMaximum() ) {
      intVal = this.progressBar.getMaximum();
    }
    this.progressBar.setValue( intVal );
  }


  private void updVolumeSliderEnabled( boolean state )
  {
    FloatControl control = null;
    if( state && (this.audioIO != null) ) {
      Control[] controls = null;
      if( this.audioIO.supportsMonitor() ) {
	controls = this.audioIO.getMonitorControls();
      } else {
	controls = this.audioIO.getDataControls();
      }
      if( controls != null ) {
	if( controls.length > 0 ) {
	  for( int i = 0; i < controls.length; i++ ) {
	    if( controls[ i ] instanceof FloatControl ) {
	      Control.Type ctrlType = controls[ i ].getType();
	      if( ctrlType != null ) {
		if( ctrlType.equals( FloatControl.Type.MASTER_GAIN ) ) {
		  control = (FloatControl) controls[ i ];
		  break;
		}
		else if( ctrlType.equals( FloatControl.Type.VOLUME ) ) {
		  control = (FloatControl) controls[ i ];
		}
	      }
	    }
	  }
	}
      }
    }
    this.controlVolume = control;

    int vMin = this.sliderVolume.getMinimum();
    if( this.controlVolume != null ) {
      int vMax = this.sliderVolume.getMaximum();
      int v    = this.lastVolumeSliderValue;
      if( (v < vMin) || (v > vMax) ) {
	v = vMin + Math.round( getNormalizedValue( this.controlVolume )
						* ((float) (vMax - vMin)) );
	if( v < vMin ) {
	  v = vMin;
	} else if( v > vMax ) {
	  v = vMax;
	}
      }
      this.sliderVolume.setValue( v );
      this.sliderVolume.setEnabled( true );
      this.panelVolume.setEnabled( true );
    } else {
      if( this.sliderVolume.isEnabled() ) {
	this.lastVolumeSliderValue = this.sliderVolume.getValue();
      }
      this.sliderVolume.setValue( vMin );
      this.sliderVolume.setEnabled( false );
      this.panelVolume.setEnabled( false );
    }
  }
}

