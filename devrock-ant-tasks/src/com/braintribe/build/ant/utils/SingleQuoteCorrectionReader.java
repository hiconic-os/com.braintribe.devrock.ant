// ============================================================================
// BRAINTRIBE TECHNOLOGY GMBH - www.braintribe.com
// Copyright BRAINTRIBE TECHNOLOGY GMBH, Austria, 2002-2018 - All Rights Reserved
// It is strictly forbidden to copy, modify, distribute or use this code without written permission
// To this file the Braintribe License Agreement applies.
// ============================================================================


package com.braintribe.build.ant.utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;

public class SingleQuoteCorrectionReader extends Reader {
	private Reader delegate;
	private boolean eof;
	private Character parkedChar;
	
	public SingleQuoteCorrectionReader(Reader delegate) {
		this.delegate = delegate;
	}
	
	
	@Override
	public void close() throws IOException {
		delegate.close();
		
	}
	
	private int decodingRead() throws IOException {
		if (eof) return -1;
		if (parkedChar != null) {
			int retval = parkedChar;
			parkedChar = null;
			return retval;
		}
		else {
			int res = delegate.read();
			if (res == -1) return -1;
			else {
				char c = (char)res;
				
				if (c == '\'') {
					delegate.mark(1);
					res = delegate.read();
					
					if (res == -1) {
						eof = true;
					}
					else {
						char c1 = (char)res;
						if (c1 == '\'' || c1 == '{') {
							parkedChar = c1;
						}
						else {
							delegate.reset();
							parkedChar = '\'';
						}
					}
				}
				
				return c;
			}
		}
	}
	
	@Override
	public int read(char[] cbuf, int off, int len) throws IOException {
		if (eof) return -1;

		int i = 0;
		
		while (i < len) {
			
			int res = decodingRead();
			
			if (res == -1) {
				if (i == 0) return -1;
				else break;
			}
			else {
				char c = (char)res;
				cbuf[off + i++] = c;
			}
		}
		
		return i;
	}
	
	
	public static void main(String[] args) {
		String text = "Hallo'{ Welt!'";
		StringReader reader = new StringReader(text);
		BufferedReader bufferedReader = null;
		try {
			SingleQuoteCorrectionReader correctionReader = new SingleQuoteCorrectionReader(reader);
			bufferedReader = new BufferedReader(correctionReader);
			
			String line = null;
			
			while ((line = bufferedReader.readLine()) != null) {
				System.out.println(line);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		finally {
			if (bufferedReader != null)
				try {
					bufferedReader.close();
				} catch (IOException e) {
					;
				}
		}
	}
}
