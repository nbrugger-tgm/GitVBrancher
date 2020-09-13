package com.niton.gitbrancher.model;

import com.niton.media.IOUntility;
import org.mapdb.DataInput2;
import org.mapdb.DataOutput2;
import org.mapdb.Serializer;

import java.io.DataInput;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;

public class OOSSerializer<T extends Serializable> implements Serializer<T>{

	@Override
	public void serialize( DataOutput2 out, T value) throws IOException {
		out.write(IOUntility.serialize(value));
	}

	@Override
	public T deserialize( DataInput2 input, int available) throws IOException {
		try {
			return IOUntility.deSerialize(new DataInputStream(input));
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
			return null;
		}
	}
	private class DataInputStream extends InputStream {
		private DataInput src;

		private DataInputStream(DataInput src) {
			this.src = src;
		}

		@Override
		public int read() throws IOException {
			return src.readByte();
		}
	}
}
