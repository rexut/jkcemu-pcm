/*
 * (c) 2009 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Emulation des Schach- und Lerncomputers SLC1
 */

package jkcemu.system;

import java.awt.*;
import java.awt.event.KeyEvent;
import java.lang.*;
import java.util.*;
import jkcemu.base.*;
import z80emu.*;


public class SLC1 extends EmuSys implements
					Z80MaxSpeedListener,
					Z80PCListener,
					Z80TStatesListener
{
  private static byte[] rom = null;

  private byte[]  ram;
  private int[]   keyValues;
  private int[]   digitStatus;
  private int[]   digitValues;
  private int     displayTStates;
  private int     curKeyCol;
  private int     curSegValue;
  private long    curDisplayTStates;
  private int     ledStatus;
  private boolean ledValue;
  private boolean chessMode;


  public SLC1( EmuThread emuThread, Properties props )
  {
    super( emuThread, props );
    if( rom == null ) {
      rom = readResource( "/rom/slc1/slc1_0000.bin" );
    }
    this.ram         = new byte[ 0x0400 ];
    this.keyValues   = new int[ 3 ];
    this.digitStatus = new int[ 6 ];
    this.digitValues = new int[ 6 ];

    Z80CPU cpu = emuThread.getZ80CPU();
    cpu.addMaxSpeedListener( this );
    cpu.addPCListener( this, 0x0000, 0x0615 );
    cpu.addTStatesListener( this );

    reset( EmuThread.ResetLevel.POWER_ON, props );
    z80MaxSpeedChanged( cpu );
  }


  public static int getDefaultSpeedKHz()
  {
    return 2500;
  }


	/* --- Z80MaxSpeedListener --- */

  public void z80MaxSpeedChanged( Z80CPU cpu )
  {
    this.displayTStates = cpu.getMaxSpeedKHz() * 50;
  }


	/* --- Z80PCListener --- */

  public void z80PCChanged( Z80CPU cpu, int pc )
  {
    switch( pc ) {
      case 0x0000:
	this.chessMode = true;
	break;

      case 0x0615:
	this.chessMode = false;
	break;
    }
  }


	/* --- Z80TStatesListener --- */

  public void z80TStatesProcessed( Z80CPU cpu, int tStates )
  {
    if( this.displayTStates > 0 ) {
      this.curDisplayTStates += tStates;
      if( this.curDisplayTStates > this.displayTStates ) {
	boolean dirty = false;
	synchronized( this.digitStatus ) {
	  for( int i = 0; i < this.digitStatus.length; i++ ) {
	    if( this.digitStatus[ i ] > 0 ) {
	      --this.digitStatus[ i ];
	    } else {
	      if( this.digitValues[ i ] != 0 ) {
		this.digitValues[ i ] = 0;
		dirty = true;
	      }
	    }
	  }
	  if( !this.ledValue && (this.ledStatus > 0) ) {
	    --this.ledStatus;
	    if( this.ledStatus == 0 ) {
	      dirty = true;
	    }
	  }
	}
	if( dirty ) {
	  this.screenFrm.setScreenDirty( true );
	}
	this.curDisplayTStates = 0;
      }
    }
  }


	/* --- ueberschriebene Methoden --- */

  public void die()
  {
    Z80CPU cpu = this.emuThread.getZ80CPU();
    cpu.removeTStatesListener( this );
    cpu.removePCListener( this );
    cpu.removeMaxSpeedListener( this );
  }


  public Chessman getChessman( int row, int col )
  {
    Chessman rv = null;
    if( this.chessMode
	&& (row >= 0) && (row < 8) && (col >= 0) && (col < 8) )
    {
      switch( getMemByte( 0x5000 + (row * 16) + col, false ) ) {
	case 1:
	  rv = Chessman.WHITE_PAWN;
	  break;

	case 2:
	  rv = Chessman.WHITE_KNIGHT;
	  break;

	case 3:
	  rv = Chessman.WHITE_BISHOP;
	  break;

	case 4:
	  rv = Chessman.WHITE_ROOK;
	  break;

	case 5:
	  rv = Chessman.WHITE_QUEEN;
	  break;

	case 6:
	  rv = Chessman.WHITE_KING;
	  break;

	case 0xFF:
	  rv = Chessman.BLACK_PAWN;
	  break;

	case 0xFE:
	  rv = Chessman.BLACK_KNIGHT;
	  break;

	case 0xFD:
	  rv = Chessman.BLACK_BISHOP;
	  break;

	case 0xFC:
	  rv = Chessman.BLACK_ROOK;
	  break;

	case 0xFB:
	  rv = Chessman.BLACK_QUEEN;
	  break;

	case 0xFA:
	  rv = Chessman.BLACK_KING;
	  break;
      }
    }
    return rv;
  }


  public Color getColor( int colorIdx )
  {
    Color color = Color.black;
    switch( colorIdx ) {
      case 1:
	color = this.colorGreenDark;
	break;

      case 2:
	color = this.colorGreenLight;
	break;
    }
    return color;
  }


  public int getColorCount()
  {
    return 3;
  }


  public String getHelpPage()
  {
    return "/help/slc1.htm";
  }


  public int getMemByte( int addr, boolean m1 )
  {
    int rv = 0xFF;
    if( (addr & 0x4000) != 0 ) {        // A14=0: ROM, A14=1: RAM
      int idx = addr & 0x03FF;
      if( idx < this.ram.length ) {
	rv = (int) this.ram[ idx ] & 0xFF;
      }
    } else {
      addr &= 0x0FFF;
      if( addr < rom.length ) {
	rv = (int) rom[ addr ] & 0xFF;
      }
    }
    return rv;
  }


  public int getScreenHeight()
  {
    return 110;
  }


  public int getScreenWidth()
  {
    return 70 + (this.digitValues.length * 50);
  }


  public String getTitle()
  {
    return "SLC1";
  }


  public boolean keyPressed( int keyCode, boolean shiftDown )
  {
    boolean rv = false;
    synchronized( this.keyValues ) {
      if( this.chessMode ) {
	switch( keyCode ) {
	  case KeyEvent.VK_ESCAPE:	// Rueckstellen
	    this.keyValues[ 2 ] = 0xEF;
	    rv = true;
	    break;

	  case KeyEvent.VK_ENTER:	// Figurenwahl, Zug quittieren
	    this.keyValues[ 2 ] = 0x7F;
	    rv = true;
	    break;

	  case KeyEvent.VK_F1:		// in Spielmode wechseln
	    this.keyValues[ 2 ] = 0xBF;
	    rv = true;
	    break;

	  case KeyEvent.VK_F2:		// Spielstaerke
	    this.keyValues[ 2 ] = 0xDF;
	    rv = true;
	    break;
	}
      } else {
	switch( keyCode ) {
	  case KeyEvent.VK_F1:		// ADR
	    this.keyValues[ 2 ] = 0x7F;
	    rv = true;
	    break;

	  case KeyEvent.VK_F2:		// Fu
	    this.keyValues[ 2 ] = 0xBF;
	    rv = true;
	    break;
	}
      }
    }
    return rv;
  }


  public void keyReleased()
  {
    synchronized( this.keyValues ) {
      Arrays.fill( this.keyValues, 0xFF );
    }
  }


  public boolean keyTyped( char keyChar )
  {
    boolean rv = false;
    synchronized( this.keyValues ) {
      keyChar = Character.toUpperCase( keyChar );
      if( this.chessMode ) {
	switch( keyChar ) {
	  case 'S':			// in Spielmode wechseln
	    this.keyValues[ 2 ] = 0xBF;
	    rv = true;
	    break;

	  case 'Z':			// Figurenwahl, Zug quittieren
	    this.keyValues[ 2 ] = 0x7F;
	    rv = true;
	    break;

	  case 'A':			// Spalte A bzw. Zeile 1
	  case '1':
	    this.keyValues[ 0 ] = 0x7F;
	    rv = true;
	    break;

	  case 'B':			// Spalte B bzw. Zeile 2
	  case '2':
	    this.keyValues[ 0 ] = 0xBF;
	    rv = true;
	    break;

	  case 'C':			// Spalte C bzw. Zeile 3
	  case '3':
	    this.keyValues[ 0 ] = 0xDF;
	    rv = true;
	    break;

	  case 'D':			// Spalte D bzw. Zeile 4
	  case '4':
	    this.keyValues[ 0 ] = 0xEF;
	    rv = true;
	    break;

	  case 'E':			// Spalte E bzw. Zeile 5
	  case '5':
	    this.keyValues[ 1 ] = 0xEF;
	    rv = true;
	    break;

	  case 'F':			// Spalte F bzw. Zeile 6
	  case '6':
	    this.keyValues[ 1 ] = 0xDF;
	    rv = true;
	    break;

	  case 'G':			// Spalte G bzw. Zeile 7
	  case '7':
	    this.keyValues[ 1 ] = 0xBF;
	    rv = true;
	    break;

	  case 'H':			// Spalte H bzw. Zeile 8
	  case '8':
	    this.keyValues[ 1 ] = 0x7F;
	    rv = true;
	    break;
	}
      } else {
	switch( keyChar ) {
	  case '0':			// 0 bzw. 8
	  case '8':
	    this.keyValues[ 0 ] = 0x7F;
	    rv = true;
	    break;

	  case '1':			// 1 bzw. 9
	  case '9':
	    this.keyValues[ 0 ] = 0xBF;
	    rv = true;
	    break;

	  case '2':			// 2 bzw. A
	  case 'A':
	    this.keyValues[ 0 ] = 0xDF;
	    rv = true;
	    break;

	  case '3':			// 3 bzw. B
	  case 'B':
	    this.keyValues[ 0 ] = 0xEF;
	    rv = true;
	    break;

	  case '4':			// 4 bzw. C
	  case 'C':
	    this.keyValues[ 1 ] = 0xEF;
	    rv = true;
	    break;

	  case '5':			// 5 bzw. D
	  case 'D':
	    this.keyValues[ 1 ] = 0xDF;
	    rv = true;
	    break;

	  case '6':			// 6 bzw. E
	  case 'E':
	    this.keyValues[ 1 ] = 0xBF;
	    rv = true;
	    break;

	  case '7':			// 7 bzw. F
	  case 'F':
	    this.keyValues[ 1 ] = 0x7F;
	    rv = true;
	    break;

	  case 'S':			// Shift
	    this.keyValues[ 2 ] = 0xEF;
	    rv = true;
	    break;

	  case '+':			// +/- 1
	  case '-':
	    this.keyValues[ 2 ] = 0xDF;
	    rv = true;
	    break;
	}
      }
    }
    return rv;
  }


  public boolean paintScreen( Graphics g, int x, int y, int screenScale )
  {
    synchronized( this.digitStatus ) {

      // LED Busy
      g.setFont( new Font( "SansSerif", Font.BOLD, 18 * screenScale ) );
      g.setColor( this.ledValue || (this.ledStatus > 0) ?
					this.colorGreenLight
					: this.colorGreenDark );
      g.drawString( "Busy", x, y + (110 * screenScale) );

      // 7-Segment-Anzeige
      for( int i = this.digitValues.length - 1; i >= 0; --i ) {
	paint7SegDigit(
		g,
		x,
		y,
		this.digitValues[ i ],
		this.colorGreenDark,
		this.colorGreenLight,
		screenScale );
	x += (65 * screenScale);
      }
    }
    return true;
  }


  public int readIOByte( int port )
  {
    int rv = 0xFF;
    synchronized( this.keyValues ) {
      if( (this.curKeyCol >= 0)
	  && (this.curKeyCol < this.keyValues.length) )
      {
	rv = this.keyValues[ this.curKeyCol ];
      }
    }
    return rv;
  }


  /*
   * Ein RESET ist erforderlich, wenn sich das emulierte System aendert.
   */
  public boolean requiresReset( Properties props )
  {
    return !EmuUtil.getProperty( props, "jkcemu.system" ).equals( "SLC1" );
  }


  public void reset( EmuThread.ResetLevel resetLevel, Properties props )
  {
    if( resetLevel == EmuThread.ResetLevel.POWER_ON ) {
      initSRAM( this.ram, props );
    }
    synchronized( this.keyValues ) {
      Arrays.fill( this.keyValues, 0xFF );
      this.curKeyCol = 0;
    }
    synchronized( this.digitStatus ) {
      Arrays.fill( this.digitStatus, 0 );
      Arrays.fill( this.digitValues, 0 );
      this.ledStatus = 0;
      this.ledValue  = false;
    }
    this.curSegValue       = 0;
    this.curDisplayTStates = 0;
    this.chessMode         = true;
  }


  public boolean setMemByte( int addr, int value )
  {
    boolean rv = false;
    if( (addr & 0x4000) != 0 ) {        // A14=1: RAM
      int idx = addr & 0x03FF;
      if( idx < this.ram.length ) {
	this.ram[ idx ] = (byte) value;
	if( this.chessMode && (idx < 0x78) && ((idx % 16) < 8) ) {
	  this.screenFrm.setChessboardDirty( true );
	}
	rv = true;
      }
    }
    return rv;
  }


  public boolean supportsChessboard()
  {
    return true;
  }


  public void writeIOByte( int port, int value )
  {
    // Tastaturspalten
    int col = value & 0x0F;
    synchronized( this.keyValues ) {
      this.curKeyCol = col - 3;
    }

    // Anzeige
    boolean dirty   = false;
    int     segMask = 0x01;
    int     segNum  = port & 0x07;
    if( segNum > 0 ) {
      segMask <<= segNum;
    }
    if( (value & 0x80) != 0 ) {
      this.curSegValue |= segMask;
    } else {
      this.curSegValue &= ~segMask;
    }
    if( col == 2 ) {
      col = -1;
    }
    else if( col > 2 ) {
      --col;
    }
    synchronized( this.digitStatus ) {
      if( (col >= 0) && (col < this.digitValues.length) ) {
	this.curSegValue &= 0x7F;
	if( this.curSegValue != this.digitValues[ col ] ) {
	  this.digitValues[ col ] = this.curSegValue;
	  dirty = true;
	}
	this.digitStatus[ col ] = 3;
      }
      int     ledStatus = 0;
      boolean ledValue  = ((value & 0x10) != 0);
      if( ledValue ) {
	ledStatus = 3;
	if( !this.ledValue && (this.ledStatus == 0) ) {
	  dirty = true;
	}
      } else {
	if( this.ledValue || (this.ledStatus > 0) ) {
	  dirty = true;
	}
      }
      this.ledStatus = ledStatus;
      this.ledValue  = ledValue;
    }
    if( dirty ) {
      this.screenFrm.setScreenDirty( true );
    }
  }
}

