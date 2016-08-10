package org.apache.apex.malhar.lib.protobuf;

import java.io.ByteArrayOutputStream;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;

import com.datatorrent.api.Context;
import com.datatorrent.api.Operator.OutputPort;
import com.datatorrent.api.Sink;
import com.datatorrent.lib.testbench.CollectorTestSink;
import com.datatorrent.netlet.util.Slice;
import com.example.tutorial.AddressBookProtos.AddressBook;
import com.example.tutorial.AddressBookProtos.Person;


public class ProtobufProcessorTest
{
  public static class TestMeta extends TestWatcher
  {
    
    ProtobufProcessor<AddressBook> operator;
    Context.OperatorContext context;
    CollectorTestSink<Object> validDataSink;
    CollectorTestSink<String> invalidDataSink;
    
    @SuppressWarnings({"unchecked", "rawtypes"})
    public static <S extends Sink, T> S setSink(OutputPort<T> port, S sink)
    {
      port.setSink(sink);
      return sink;
    }
    
    @Override
    protected void starting(Description description)
    {
      super.starting(description);
      operator = new ProtobufProcessor<AddressBook>();
      operator.setClazz(AddressBook.class);
      operator.setParser(com.example.tutorial.AddressBookProtos.AddressBook.PARSER);

      validDataSink = new CollectorTestSink<Object>();
      invalidDataSink = new CollectorTestSink<String>();
      operator.out.setSink(validDataSink);
      setSink(operator.err, invalidDataSink);
    }

    @Override
    protected void finished(Description description)
    {
      super.finished(description);
      operator.teardown();
    }
    
  }
  
  @Rule
  public TestMeta testMeta = new TestMeta();

  /**
   * Basic protobuf read test
   *
   * @throws Exception
   */
  @Test
  public void testSimpleAddressBook() throws Exception
  {
    AddressBook.Builder addressBook = AddressBook.newBuilder();
    
    // add a couple of entries                                                                                                                                              
    addressBook.addPerson(buildPerson(1, "John Smith", "john@dom1.com", "123-456-7890", null, "800-555-1212"));
    addressBook.addPerson(buildPerson(1, "James Ford", "james@dom2.com", "456-123-7891", "890-555-1212", "987-654-3210"));
    ByteArrayOutputStream output = new ByteArrayOutputStream();
    AddressBook orig = addressBook.build();
    
    orig.writeTo(output);
    
    testMeta.operator.setup(null);
    Slice slice = new Slice(output.toByteArray(), 0, output.size());
    testMeta.operator.in.process(slice);
    
    Assert.assertEquals(1, testMeta.validDataSink.collectedTuples.size());
    Assert.assertEquals(0, testMeta.invalidDataSink.collectedTuples.size());
    Object obj = testMeta.validDataSink.collectedTuples.get(0);
    Assert.assertNotNull(obj);
    Assert.assertEquals(AddressBook.class, obj.getClass());
    AddressBook pojo = (AddressBook)obj;
    
    Assert.assertEquals(orig, pojo);
  }

  private static void addPhone(Person.Builder person, String number, Person.PhoneType type)
  {
    if (number != null) {
      Person.PhoneNumber.Builder phoneNumber =
          Person.PhoneNumber.newBuilder().setNumber(number);
      phoneNumber.setType(type);
      person.addPhone(phoneNumber);
    }
  }
  
  
  private static Person buildPerson(int id, String name, String email,
      String mobile, String home, String work)
  {
    Person.Builder person = Person.newBuilder();

    person.setId(id);
    person.setName(name);
    person.setEmail(email);
    addPhone(person, mobile, Person.PhoneType.MOBILE);
    addPhone(person, home, Person.PhoneType.HOME);
    addPhone(person, work, Person.PhoneType.WORK);

    return person.build();
  }
  
}
