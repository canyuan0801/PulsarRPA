/**
 * Autogenerated by Avro
 *
 * DO NOT EDIT DIRECTLY
 */
package ai.platon.pulsar.persist.gora.generated;  

/**
 * <p>GHypeLink class.</p>
 *
 * @author vincent
 * @version $Id: $Id
 */
public class GHypeLink extends org.apache.gora.persistency.impl.PersistentBase implements org.apache.avro.specific.SpecificRecord, org.apache.gora.persistency.Persistent {
  /** Constant <code>SCHEMA$</code> */
  public static final org.apache.avro.Schema SCHEMA$ = new org.apache.avro.Schema.Parser().parse("{\"type\":\"record\",\"name\":\"GHypeLink\",\"namespace\":\"ai.platon.pulsar.persist.gora.generated\",\"fields\":[{\"name\":\"url\",\"type\":\"string\",\"default\":\"\"},{\"name\":\"anchor\",\"type\":[\"null\",\"string\"],\"default\":null},{\"name\":\"order\",\"type\":\"int\",\"default\":0}]}");
  private static final long serialVersionUID = -7365210677982337286L;
  /** Enum containing all data bean's fields. */
  public static enum Field {
    URL(0, "url"),
    ANCHOR(1, "anchor"),
    ORDER(2, "order"),
    ;
    /**
     * Field's index.
     */
    private int index;

    /**
     * Field's name.
     */
    private String name;

    /**
     * Field's constructor
     * @param index field's index.
     * @param name field's name.
     */
    Field(int index, String name) {this.index=index;this.name=name;}

    /**
     * Gets field's index.
     * @return int field's index.
     */
    public int getIndex() {return index;}

    /**
     * Gets field's name.
     * @return String field's name.
     */
    public String getName() {return name;}

    /**
     * Gets field's attributes to string.
     * @return String field's attributes to string.
     */
    public String toString() {return name;}
  };

  /** Constant <code>_ALL_FIELDS</code> */
  public static final String[] _ALL_FIELDS = {
  "url",
  "anchor",
  "order",
  };

  /**
   * Gets the total field count.
   *
   * @return int field count
   */
  public int getFieldsCount() {
    return GHypeLink._ALL_FIELDS.length;
  }

  private CharSequence url;
  private CharSequence anchor;
  private int order;
  /**
   * <p>getSchema.</p>
   *
   * @return a {@link org.apache.avro.Schema} object.
   */
  public org.apache.avro.Schema getSchema() { return SCHEMA$; }
  // Used by DatumWriter.  Applications should not call.
  /** {@inheritDoc} */
  public Object get(int field$) {
    switch (field$) {
    case 0: return url;
    case 1: return anchor;
    case 2: return order;
    default: throw new org.apache.avro.AvroRuntimeException("Bad index");
    }
  }

  // Used by DatumReader.  Applications should not call.
  /** {@inheritDoc} */
  @SuppressWarnings(value="unchecked")
  public void put(int field$, Object value) {
    switch (field$) {
    case 0: url = (CharSequence)(value); break;
    case 1: anchor = (CharSequence)(value); break;
    case 2: order = (Integer)(value); break;
    default: throw new org.apache.avro.AvroRuntimeException("Bad index");
    }
  }

  /**
   * Gets the value of the 'url' field.
   *
   * @return a {@link java.lang.CharSequence} object.
   */
  public CharSequence getUrl() {
    return url;
  }

  /**
   * Sets the value of the 'url' field.
   *
   * @param value the value to set.
   */
  public void setUrl(CharSequence value) {
    this.url = value;
    setDirty(0);
  }

  /**
   * Checks the dirty status of the 'url' field. A field is dirty if it represents a change that has not yet been written to the database.
   *
   * @return a boolean.
   */
  public boolean isUrlDirty() {
    return isDirty(0);
  }

  /**
   * Gets the value of the 'anchor' field.
   *
   * @return a {@link java.lang.CharSequence} object.
   */
  public CharSequence getAnchor() {
    return anchor;
  }

  /**
   * Sets the value of the 'anchor' field.
   *
   * @param value the value to set.
   */
  public void setAnchor(CharSequence value) {
    this.anchor = value;
    setDirty(1);
  }

  /**
   * Checks the dirty status of the 'anchor' field. A field is dirty if it represents a change that has not yet been written to the database.
   *
   * @return a boolean.
   */
  public boolean isAnchorDirty() {
    return isDirty(1);
  }

  /**
   * Gets the value of the 'order' field.
   *
   * @return a {@link java.lang.Integer} object.
   */
  public Integer getOrder() {
    return order;
  }

  /**
   * Sets the value of the 'order' field.
   *
   * @param value the value to set.
   */
  public void setOrder(Integer value) {
    this.order = value;
    setDirty(2);
  }

  /**
   * Checks the dirty status of the 'order' field. A field is dirty if it represents a change that has not yet been written to the database.
   *
   * @return a boolean.
   */
  public boolean isOrderDirty() {
    return isDirty(2);
  }

  /**
   * Creates a new GHypeLink RecordBuilder
   *
   * @return a {@link ai.platon.pulsar.persist.gora.generated.GHypeLink.Builder} object.
   */
  public static GHypeLink.Builder newBuilder() {
    return new GHypeLink.Builder();
  }

  /**
   * Creates a new GHypeLink RecordBuilder by copying an existing Builder
   *
   * @param other a {@link ai.platon.pulsar.persist.gora.generated.GHypeLink.Builder} object.
   * @return a {@link ai.platon.pulsar.persist.gora.generated.GHypeLink.Builder} object.
   */
  public static GHypeLink.Builder newBuilder(GHypeLink.Builder other) {
    return new GHypeLink.Builder(other);
  }

  /**
   * Creates a new GHypeLink RecordBuilder by copying an existing GHypeLink instance
   *
   * @param other a {@link ai.platon.pulsar.persist.gora.generated.GHypeLink} object.
   * @return a {@link ai.platon.pulsar.persist.gora.generated.GHypeLink.Builder} object.
   */
  public static GHypeLink.Builder newBuilder(GHypeLink other) {
    return new GHypeLink.Builder(other);
  }

  private static java.nio.ByteBuffer deepCopyToReadOnlyBuffer(
      java.nio.ByteBuffer input) {
    java.nio.ByteBuffer copy = java.nio.ByteBuffer.allocate(input.capacity());
    int position = input.position();
    input.reset();
    int mark = input.position();
    int limit = input.limit();
    input.rewind();
    input.limit(input.capacity());
    copy.put(input);
    input.rewind();
    copy.rewind();
    input.position(mark);
    input.mark();
    copy.position(mark);
    copy.mark();
    input.position(position);
    copy.position(position);
    input.limit(limit);
    copy.limit(limit);
    return copy.asReadOnlyBuffer();
  }

  /**
   * RecordBuilder for GHypeLink instances.
   */
  public static class Builder extends org.apache.avro.specific.SpecificRecordBuilderBase<GHypeLink>
    implements org.apache.avro.data.RecordBuilder<GHypeLink> {

    private CharSequence url;
    private CharSequence anchor;
    private int order;

    /** Creates a new Builder */
    private Builder() {
      super(GHypeLink.SCHEMA$);
    }

    /** Creates a Builder by copying an existing Builder */
    private Builder(GHypeLink.Builder other) {
      super(other);
    }

    /** Creates a Builder by copying an existing GHypeLink instance */
    private Builder(GHypeLink other) {
            super(GHypeLink.SCHEMA$);
      if (isValidValue(fields()[0], other.url)) {
        this.url = (CharSequence) data().deepCopy(fields()[0].schema(), other.url);
        fieldSetFlags()[0] = true;
      }
      if (isValidValue(fields()[1], other.anchor)) {
        this.anchor = (CharSequence) data().deepCopy(fields()[1].schema(), other.anchor);
        fieldSetFlags()[1] = true;
      }
      if (isValidValue(fields()[2], other.order)) {
        this.order = (Integer) data().deepCopy(fields()[2].schema(), other.order);
        fieldSetFlags()[2] = true;
      }
    }

    /** Gets the value of the 'url' field */
    public CharSequence getUrl() {
      return url;
    }

    /** Sets the value of the 'url' field */
    public GHypeLink.Builder setUrl(CharSequence value) {
      validate(fields()[0], value);
      this.url = value;
      fieldSetFlags()[0] = true;
      return this;
    }

    /** Checks whether the 'url' field has been set */
    public boolean hasUrl() {
      return fieldSetFlags()[0];
    }

    /** Clears the value of the 'url' field */
    public GHypeLink.Builder clearUrl() {
      url = null;
      fieldSetFlags()[0] = false;
      return this;
    }

    /** Gets the value of the 'anchor' field */
    public CharSequence getAnchor() {
      return anchor;
    }

    /** Sets the value of the 'anchor' field */
    public GHypeLink.Builder setAnchor(CharSequence value) {
      validate(fields()[1], value);
      this.anchor = value;
      fieldSetFlags()[1] = true;
      return this;
    }

    /** Checks whether the 'anchor' field has been set */
    public boolean hasAnchor() {
      return fieldSetFlags()[1];
    }

    /** Clears the value of the 'anchor' field */
    public GHypeLink.Builder clearAnchor() {
      anchor = null;
      fieldSetFlags()[1] = false;
      return this;
    }

    /** Gets the value of the 'order' field */
    public Integer getOrder() {
      return order;
    }

    /** Sets the value of the 'order' field */
    public GHypeLink.Builder setOrder(int value) {
      validate(fields()[2], value);
      this.order = value;
      fieldSetFlags()[2] = true;
      return this;
    }

    /** Checks whether the 'order' field has been set */
    public boolean hasOrder() {
      return fieldSetFlags()[2];
    }

    /** Clears the value of the 'order' field */
    public GHypeLink.Builder clearOrder() {
      fieldSetFlags()[2] = false;
      return this;
    }

    @Override
    public GHypeLink build() {
      try {
        GHypeLink record = new GHypeLink();
        record.url = fieldSetFlags()[0] ? this.url : (CharSequence) defaultValue(fields()[0]);
        record.anchor = fieldSetFlags()[1] ? this.anchor : (CharSequence) defaultValue(fields()[1]);
        record.order = fieldSetFlags()[2] ? this.order : (Integer) defaultValue(fields()[2]);
        return record;
      } catch (Exception e) {
        throw new org.apache.avro.AvroRuntimeException(e);
      }
    }
  }

  /**
   * <p>getTombstone.</p>
   *
   * @return a {@link ai.platon.pulsar.persist.gora.generated.GHypeLink.Tombstone} object.
   */
  public Tombstone getTombstone(){
  	return TOMBSTONE;
  }

  /**
   * <p>newInstance.</p>
   *
   * @return a {@link ai.platon.pulsar.persist.gora.generated.GHypeLink} object.
   */
  public GHypeLink newInstance(){
    return newBuilder().build();
  }

  private static final Tombstone TOMBSTONE = new Tombstone();

  public static final class Tombstone extends GHypeLink implements org.apache.gora.persistency.Tombstone {

      private Tombstone() { }

	  		  /**
	   * Gets the value of the 'url' field.
		   */
	  public CharSequence getUrl() {
	    throw new UnsupportedOperationException("Get is not supported on tombstones");
	  }

	  /**
	   * Sets the value of the 'url' field.
		   * @param value the value to set.
	   */
	  public void setUrl(CharSequence value) {
	    throw new UnsupportedOperationException("Set is not supported on tombstones");
	  }

	  /**
	   * Checks the dirty status of the 'url' field. A field is dirty if it represents a change that has not yet been written to the database.
		   * @param value the value to set.
	   */
	  public boolean isUrlDirty() {
	    throw new UnsupportedOperationException("IsDirty is not supported on tombstones");
	  }

				  /**
	   * Gets the value of the 'anchor' field.
		   */
	  public CharSequence getAnchor() {
	    throw new UnsupportedOperationException("Get is not supported on tombstones");
	  }

	  /**
	   * Sets the value of the 'anchor' field.
		   * @param value the value to set.
	   */
	  public void setAnchor(CharSequence value) {
	    throw new UnsupportedOperationException("Set is not supported on tombstones");
	  }

	  /**
	   * Checks the dirty status of the 'anchor' field. A field is dirty if it represents a change that has not yet been written to the database.
		   * @param value the value to set.
	   */
	  public boolean isAnchorDirty() {
	    throw new UnsupportedOperationException("IsDirty is not supported on tombstones");
	  }

				  /**
	   * Gets the value of the 'order' field.
		   */
	  public Integer getOrder() {
	    throw new UnsupportedOperationException("Get is not supported on tombstones");
	  }

	  /**
	   * Sets the value of the 'order' field.
		   * @param value the value to set.
	   */
	  public void setOrder(Integer value) {
	    throw new UnsupportedOperationException("Set is not supported on tombstones");
	  }

	  /**
	   * Checks the dirty status of the 'order' field. A field is dirty if it represents a change that has not yet been written to the database.
		   * @param value the value to set.
	   */
	  public boolean isOrderDirty() {
	    throw new UnsupportedOperationException("IsDirty is not supported on tombstones");
	  }
	
		  
  }

  private static final org.apache.avro.io.DatumWriter
            DATUM_WRITER$ = new org.apache.avro.specific.SpecificDatumWriter(SCHEMA$);
  private static final org.apache.avro.io.DatumReader
            DATUM_READER$ = new org.apache.avro.specific.SpecificDatumReader(SCHEMA$);

  /**
   * {@inheritDoc}
   *
   * Writes AVRO data bean to output stream in the form of AVRO Binary encoding format. This will transform
   * AVRO data bean from its Java object form to it s serializable form.
   */
  @Override
  public void writeExternal(java.io.ObjectOutput out)
          throws java.io.IOException {
    out.write(super.getDirtyBytes().array());
    DATUM_WRITER$.write(this, org.apache.avro.io.EncoderFactory.get()
            .directBinaryEncoder((java.io.OutputStream) out,
                    null));
  }

  /**
   * {@inheritDoc}
   *
   * Reads AVRO data bean from input stream in it s AVRO Binary encoding format to Java object format.
   * This will transform AVRO data bean from it s serializable form to deserialized Java object form.
   */
  @Override
  public void readExternal(java.io.ObjectInput in)
          throws java.io.IOException {
    byte[] __g__dirty = new byte[getFieldsCount()];
    in.read(__g__dirty);
    super.setDirtyBytes(java.nio.ByteBuffer.wrap(__g__dirty));
    DATUM_READER$.read(this, org.apache.avro.io.DecoderFactory.get()
            .directBinaryDecoder((java.io.InputStream) in,
                    null));
  }
  
}

