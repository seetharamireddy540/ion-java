// Copyright (c) 2010-2013 Amazon.com, Inc.  All rights reserved.

package com.amazon.ion.impl.lite;

import com.amazon.ion.IonClob;
import com.amazon.ion.IonType;
import com.amazon.ion.IonWriter;
import com.amazon.ion.SymbolTable;
import com.amazon.ion.ValueVisitor;
import com.amazon.ion.impl._Private_Utils;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.Charset;

/**
 *
 */
final class IonClobLite
    extends IonLobLite
    implements IonClob
{
    static private final int HASH_SIGNATURE =
        IonType.CLOB.toString().hashCode();

    /**
     * Constructs a <code>null.clob</code> element.
     */
    public IonClobLite(IonSystemLite system, boolean isNull)
    {
        super(system, isNull);
    }

    IonClobLite(IonClobLite existing, IonContext context)
    {
        super(existing, context);
    }

    @Override
    IonClobLite clone(IonContext context)
    {
        return new IonClobLite(this, context);
    }

    @Override
    public IonClobLite clone()
    {
        return clearFieldName(this.clone(getSystem()));
    }

    @Override
    int hashCode(SymbolTable symbolTable) {
        return lobHashCode(HASH_SIGNATURE, symbolTable);
    }

    @Override
    public IonType getType()
    {
        return IonType.CLOB;
    }


    public Reader newReader(Charset cs)
    {
        InputStream in = newInputStream();
        if (in == null) return null;

        return new InputStreamReader(in, cs);
    }


    public String stringValue(Charset cs)
    {
        // TODO use Charset directly.
        byte[] bytes = getBytes();
        if (bytes == null) return null;

        return _Private_Utils.decode(bytes, cs);
    }

    @Override
    final void writeBodyTo(IonWriter writer, SymbolTable symbolTable)
        throws IOException
    {
        writer.writeClob(getBytesNoCopy());
    }

    @Override
    public void accept(ValueVisitor visitor) throws Exception
    {
        visitor.visit(this);
    }
}
