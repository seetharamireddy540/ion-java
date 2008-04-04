package com.amazon.ion.streaming;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Iterator;

import junit.framework.TestSuite;

import com.amazon.ion.DirectoryTestSuite;
import com.amazon.ion.FileTestCase;
import com.amazon.ion.IonDatagram;
import com.amazon.ion.IonLoader;
import com.amazon.ion.IonValue;
import com.amazon.ion.util.Equivalence;
import com.amazon.ion.util.Printer;

public class RoundTripStreamingTests extends DirectoryTestSuite
{
	static final boolean _debug_flag = false;
	
    public static TestSuite suite()
    {
        return new RoundTripStreamingTests();
    }

    public RoundTripStreamingTests()
    {
        super("good");
    }

    @Override
    protected FileTestCase makeTest(File ionFile)
    {
        String fileName = ionFile.getName();
        // this test is here to get rid of the warning, and ... you never know
        if (fileName == null || fileName.length() < 1) throw new IllegalArgumentException("files should have names");
        return new StreamingRoundTripTest(ionFile);
    }
    
    private static class StreamingRoundTripTest
    extends FileTestCase
    {
        private Printer       myPrinter;
        private StringBuilder myBuilder;
        private byte[]        myBuffer;
        
        public StreamingRoundTripTest(File ionText)
        {
            super(ionText);
            
        	FileInputStream in;
        	BufferedInputStream bin;
        	long len = ionText.length();
        	if (len < 0 || len > Integer.MAX_VALUE) throw new IllegalArgumentException("file too long for test");
        	myBuffer = new byte[(int)len];
			try {
				in = new FileInputStream(myTestFile);
				bin = new BufferedInputStream(in);
				bin.read(myBuffer);
			} catch (FileNotFoundException e) {
				throw new RuntimeException(e);
	        } catch (IOException ioe) {
	        	throw new RuntimeException(ioe);
			}

        }

        @Override
        public void setUp()
            throws Exception
        {
            super.setUp();
            myPrinter = new Printer();
            myBuilder = new StringBuilder();
        }

        @Override
        public void tearDown()
            throws Exception
        {
            myPrinter = null;
            myBuilder = null;
            myBuffer  = null;
            super.tearDown();
        }

        private String makeString(IonDatagram datagram)
            throws IOException
        {
        	boolean is_first = true;
            // iterator used to be .systemIterator()
            for (Iterator i = datagram.iterator(); i.hasNext();)
            {
                IonValue value = (IonValue) i.next();
                if (is_first) {
                	is_first = false;
                }
                else {
                	myBuilder.append(' ');
                }
                myPrinter.print(value, myBuilder);
                // Why is this here?  myBuilder.append('\n');
            }

            String text = myBuilder.toString();
            myBuilder.setLength(0);
            return text;
        }

        private byte[] makeText(byte[] buffer)
        throws IOException
        {
        	IonIterator in = makeIterator(buffer);
        	IonTextWriter tw = new IonTextWriter();
        	
        	tw.writeIonEvents(in);
        	byte[] buf = tw.getBytes(); // this is utf-8
        	
        	return buf;
        }

        private String makeString(byte[] buffer)
        throws IOException
        {
        	byte[] buf = makeText(buffer);
        	String text = new String(buf, "UTF-8");
        	
        	return text;
        }


        private byte[] makeBinary(byte[] buffer)
        throws IOException
        {
        	IonIterator in = makeIterator(buffer);
	    	IonBinaryWriter bw = new IonBinaryWriter();
	    	
			bw.writeIonEvents(in);
			byte[] buf = bw.getBytes(); // this is binary
	    	
	    	return buf;
	    }

        private byte[] makeBinary(IonDatagram datagram)
        {
            return datagram.toBytes();
        }

        private IonDatagram makeTree(byte[] buffer)
        throws IOException
        {
        	IonIterator in = makeIterator(buffer);
        	IonTreeWriter tw = new IonTreeWriter(mySystem);

        	tw.writeIonEvents(in);
        	IonValue v = tw.getContentAsIonValue();
        	
        	return (IonDatagram)v;
        }
        
        private static class roundTripBufferResults 
        {
        	roundTripBufferResults(String t) {
        		title = t;
        	}
        	String      title;
        	String 		string;
        	byte[]      utf8_buf;
        	byte[] 		binary;
        	IonDatagram ion;

        	void compareResultsPass1(roundTripBufferResults other, IonLoader loader) 
        	{
        		String vs = this.title + " vs " + other.title;
                //assertEquals("string: " + vs, this.string, other.string);
                compareStringAsTree("string: " + vs, other, loader);
                compareBuffers("utf8: " + vs, this.utf8_buf, other.utf8_buf);
                compareUTF8AsTree("utf8: " + vs, other, loader);
                compareBuffers("binary: " + vs, this.binary, other.binary);
                compareBinaryAsTree("binary: " + vs, other, loader);
                
                boolean datagrams_are_equal = Equivalence.ionEquals(this.ion, other.ion);
        		if (!datagrams_are_equal) {
        			System.out.println("\n------------------------------\n");
        			dump_binary(this.title, this.binary);
        			System.out.println("\n");
        			dump_binary(other.title, other.binary);
        			datagrams_are_equal = Equivalence.ionEquals(this.ion, other.ion);
        		}
                assertTrue("datagram: " + vs, Equivalence.ionEquals(this.ion, other.ion));
	      	}

        	void compareResults(roundTripBufferResults other, IonLoader loader) 
        	{
        		String vs = this.title + " vs " + other.title;
                assertEquals("string: " + vs, this.string, other.string);
                compareBuffers("utf8: " + vs, this.utf8_buf, other.utf8_buf);
                compareBuffers("binary: " + vs, this.binary, other.binary);
                compareBinaryAsTree("binary: " + vs, other, loader);
                assertTrue("datagram: " + vs, Equivalence.ionEquals(this.ion, other.ion));
	      	}
        	
        	static void compareBuffers(String title, byte[] buf1, byte[] buf2) {
        		assertTrue(title+" buf1 should not be null", buf1 != null);
        		assertTrue(title+" buf2 should not be null", buf2 != null);
        		
        		// TODO later, when the namespace construction is fixed
        		//             in both the iterators and the tree impl
        		// assertEquals(title + " buffer lengths", buf1.length, buf2.length);
        		// for (int ii=0; ii<buf1.length; ii++) {
        		// 	boolean bytes_are_equal = (buf1[ii] == buf2[ii]);
        		// 	assertTrue(title + " byte at "+ii, bytes_are_equal);
        		// }
        	}

        	void compareStringAsTree(String title, roundTripBufferResults other, IonLoader loader) {
        		// general approach:
        		//	    open tree over both, compare trees - use equiv
        		//      open iterators over both, compare iterator contents
        		IonDatagram dg1 = loader.load(this.string);
        		IonDatagram dg2 = loader.load(other.string);
        		
        		if (dg1 == null || dg2 == null) {
        			assertTrue("if one datagram is null both must be null in "+title, dg1 == dg2);
        		}
        		boolean datagrams_are_equal = Equivalence.ionEquals(dg1, dg2);
        		if (!datagrams_are_equal) {
        			datagrams_are_equal = Equivalence.ionEquals(dg1, dg2);
        		}
        		assertTrue("resulting datagrams should be the same in "+title, datagrams_are_equal);
        	}

        	void compareUTF8AsTree(String title, roundTripBufferResults other, IonLoader loader) {
        		// general approach:
        		//	    open tree over both, compare trees - use equiv
        		//      open iterators over both, compare iterator contents
        		IonDatagram dg1 = loader.load(this.utf8_buf);
        		IonDatagram dg2 = loader.load(other.utf8_buf);
        		
        		if (dg1 == null || dg2 == null) {
        			assertTrue("if one datagram is null both must be null in "+title, dg1 == dg2);
        		}
        		boolean datagrams_are_equal = Equivalence.ionEquals(dg1, dg2);
        		if (!datagrams_are_equal) {
        			datagrams_are_equal = Equivalence.ionEquals(dg1, dg2);
        		}
        		assertTrue("resulting datagrams should be the same in "+title, datagrams_are_equal);
        	}
        	
        	void compareBinaryAsTree(String title, roundTripBufferResults other, IonLoader loader) {
        		// general approach:
        		//	    open tree over both, compare trees - use equiv
        		//      open iterators over both, compare iterator contents
        		IonDatagram dg1 = loader.load(this.binary);
        		IonDatagram dg2 = loader.load(other.binary);
        		
        		if (dg1 == null || dg2 == null) {
        			assertTrue("if one datagram is null both must be null in "+title, dg1 == dg2);
        		}
        		boolean datagrams_are_equal = Equivalence.ionEquals(dg1, dg2);
        		if (!datagrams_are_equal) {
        			System.out.println("\n------------------------------\n");
        			dump_binary(this.title, this.binary);
        			dump_binary(other.title, other.binary);
        			datagrams_are_equal = Equivalence.ionEquals(dg1, dg2);
        		}
        		assertTrue("resulting datagrams should be the same in "+title, datagrams_are_equal);
        	}
        	
        	void dump_binary(String title, byte[] buf)
        	{        		
        		System.out.println("dump buffer for "+title);
        		if (buf == null) {
        			System.out.println(" <null>");
        		}
        		else {
        			int len = buf.length;
        			System.out.println(" length: "+len);
        	    	for (int ii=0; ii<len; ii++) {
        	        	int b = ((int)buf[ii]) & 0xff;
        	        	if ((ii & 0xf) == 0) {
        	        		System.out.println();
        	        		String x = "     "+ii;
        	        		if (x.length() > 5)  x = x.substring(x.length() - 6);
        	        		System.out.print(x+": ");
        	        	}
        	        	String y = "00"+Integer.toHexString(b);
        	        	y = y.substring(y.length() - 2);
        	        	System.out.print(y+" ");
        	        }
        	        System.out.println();
        	        
        	    	for (int ii=0; ii<len; ii++) {
        	        	int b = ((int)buf[ii]) & 0xff;
        	        	if ((ii & 0xf) == 0) {
        	        		System.out.println();
        	        		String x = "     "+ii;
        	        		if (x.length() > 5)  x = x.substring(x.length() - 6);
        	        		System.out.print(x+": ");
        	        	}
        	        	String y = "  " + (char)((b >= 32 && b < 128) ? b : ' ');
        	        	y = y.substring(y.length() - 2);
        	        	System.out.print(y+" ");
        	        }
        	        System.out.println();

	
        		}
        		
        	}
        	
        }
        
        roundTripBufferResults roundTripBuffer(String pass, byte[] testBuffer) 
        throws IOException
        {
        	roundTripBufferResults results = new roundTripBufferResults(pass+" original");
        	roundTripBufferResults in = new roundTripBufferResults(pass + " copy");
        	
            IonDatagram inputDatagram = loader().load(testBuffer);

            // Turn the DOM back into text...
            in.string   = makeString(inputDatagram);
            in.utf8_buf = in.string.getBytes("UTF-8");
            in.binary   = makeBinary(inputDatagram);
            in.ion      = inputDatagram;
            checkBinaryHeader(in.binary);            

            results.utf8_buf = makeText(testBuffer);
            results.string   = makeString(testBuffer);
            results.binary   = makeBinary(testBuffer);
            results.ion      = makeTree(testBuffer);
            
            checkBinaryHeader(results.binary);
            
            in.compareResultsPass1(results, myLoader);
            
            return results;
        }
        
        IonIterator makeIterator(byte [] testBuffer) {
        	IonIterator inputIterator = IonIterator.makeIterator(testBuffer);
        	inputIterator.hasNext();
        	return inputIterator;
        }
        
        
        @Override
        public void runTest()
            throws Exception
        {
        	// general round trip plan for the streaming interfaces:
        	//
        	//	open iterator over test file
        	//  create datagram over test file
        	//  get string from datagram
        	//  get binary from datagram
        	//  pass iterator to text writer
        	//     compare output with string from datagram
        	//  pass iterator to binary writer
        	//     compare output with binary from datagram
        	//  pass iterator to tree writer
        	//     output with datagram
        	//  test comparison again with the resulting binary
        	//  and resulting text (1 level recurse or 2?
        	
            if (this.getName().startsWith("testfile35")) {
            	// FIXME - we need to fix the problem with in line text symbol tables !!
            	if (_debug_flag) {
            		System.out.println("debugging testfile35, with in line text symbol tables");
            	}
            	else {
            		return;
            	}
        	}
        	
        	roundTripBufferResults pass1 = roundTripBuffer("P1: buffer", myBuffer);
        	roundTripBufferResults pass2bin = roundTripBuffer("P2: binary",pass1.binary);
        	roundTripBufferResults pass2text = roundTripBuffer("P3: utf8", pass1.utf8_buf);
        	if (pass2bin == pass2text) {
        		// mostly to force these to be used (pass2*)
        		throw new RuntimeException("boy this is odd");
        	}
        }
    }



}