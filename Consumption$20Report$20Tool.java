String hist = "history:|bql:select id, source where id like '*kWhTotal' and id like '/niagaraax*'";

public void onStart() throws Exception
{
  // start up code here
}

public void onExecute() throws Exception
{
  int i = 0;
  int count = 0; 
  int hTotal = 0;
  
  DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
  DateFormat dateTimeFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
  final String OLD_FORMAT = "dd-MMMM-yy";
  final String NEW_FORMAT = "MM/dd/yyyy";
  Date today = new Date();

  String[] hName = null; 
  String front = "";
  String back = "";
  String[] meterId = new String[200];
  String meterOrd = ""; 
  String[] dt = null;
  String curMeter = "";
  double thisMonthKwh = 0;
  double lastMonthKwh = 0;
  double totalKwh = 0;

  Date d = null;
  String oldDateString = "";
  String newDateString = "";
  
  FileWriter writer = new FileWriter("E:\\Niagara\\Niagara-3.7.106\\Meter Reporting\\Monthly Reports\\" + dateFormat.format(today) + "MonthEletricMeterReport.csv");
  writer.write("Device_ImportID, Date, Time, Data_Value, Interval \r\n");
    
  BOrd hOrd = BOrd.make(hist);
  BOrd lOrd;
  BITable result = (BITable)hOrd.resolve().get(); 
  ColumnList columns = result.getColumns();
  
  Column hList = columns.get(0);
  Column sList = columns.get(1); 
  
  TableCursor cNames = (TableCursor)result.cursor(); 
  TableCursor cTotals = (TableCursor)result.cursor();  

  while (cNames.next()) 
  {
    hName = TextUtil.split(cNames.get(sList).toString(), '/'); 
    meterOrd = hName[0] + "/" + hName[1] + "/" + hName[2] + "/" + hName[3] + "/" +hName[4] + "/" + hName[5] + "/meterId";
    
    curMeter = GetMeterId(meterOrd);
    meterId[count] = curMeter;
    
    count = count + 1;
  }
  
  count = 0;
  
  while (cTotals.next()) 
  {
    hOrd = BOrd.make("history:" + cTotals.get(hList).toString() + "?period=monthToDate|bql:select timestamp, value");
    lOrd = BOrd.make("history:" + cTotals.get(hList).toString() + "?period=lastMonth|bql:select timestamp, value");
    result = (BITable)hOrd.resolve().get();
    columns = result.getColumns(); 
    
    Column hDate = columns.get(0);
    Column hValue = columns.get(1);
    
    TableCursor cSample = (TableCursor)result.cursor();

    // front = cTotals.get(hList).toString().substring(0, 0);
    // back = cTotals.get(hList).toString().substring(0 + 1, cTotals.get(hList).toString().length());
    // meterId = front + back;
     
    while (cSample.next())
    { 
      dt = TextUtil.split(cSample.get(hDate).toString(), ' ');

      if (dt[0].startsWith(getDom()) && dt[1].startsWith(getTod()) && dt[2].equals(getAmpm()) && cTotals.get(hList).toString().endsWith(getTrendFilter()))
      {
        oldDateString = dt[0];
        SimpleDateFormat sdf = new SimpleDateFormat(OLD_FORMAT);
  
        d = sdf.parse(oldDateString);
        sdf.applyPattern(NEW_FORMAT);
        newDateString = sdf.format(d);
        
        thisMonthKwh = Double.parseDouble(cSample.get(hValue).toString());
        lastMonthKwh = GetLastMonth(lOrd, cTotals.get(hList).toString());
        totalKwh = thisMonthKwh - lastMonthKwh;

        if (meterId[count].toString() != "")
        {
          writer.write(meterId[count].toString() + "," + newDateString.toString() + "," + dt[1] + "," + Math.round(totalKwh) + ",900 \r\n");   
          i = i + 1;
        }   
      }
    }
    
    count = count + 1;
  }
     
  println(dateTimeFormat.format(today) + "    History report complete with " + Integer.toString(i) + " meters.");
          
  writer.flush();
  writer.close();
  
  //onSendFile();
}

public String GetMeterId(String meterOrd)
{
  String meterId;
  BOrd mOrd = BOrd.make(meterOrd);

  BString curMeter = (BString)(mOrd.resolve().get());
  meterId = curMeter.getString();

  return meterId;
}

public double GetLastMonth(BOrd lastMonthOrd, String trendName) throws Exception
{
  double lastMonthKwh = 0;
  String [] dt = null;
  BITable result = (BITable)lastMonthOrd.resolve().get(); 
  ColumnList columns = result.getColumns(); 
    
  Column hDate = columns.get(0);
  Column hValue = columns.get(1);
    
  TableCursor cSample = (TableCursor)result.cursor(); 
  
  while (cSample.next())
  { 
    dt = TextUtil.split(cSample.get(hDate).toString(), ' ');

    if (dt[0].startsWith(getDom()) && dt[1].startsWith(getTod()) && dt[2].equals(getAmpm()) && trendName.endsWith(getTrendFilter()))
    {
      lastMonthKwh = Double.parseDouble(cSample.get(hValue).toString());
    }
  }
    
  return lastMonthKwh;
}

public void onSendFile() throws Exception
{
        Date today = new Date();
        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        DateFormat dateTimeFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
  
        Multipart multipart = new MimeMultipart();
        final String username = "mrupperman@gmail.com";
        final String password = getSmtpPwd().getValue();

        Properties props = new Properties();
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.host", "smtp.gmail.com");
        props.put("mail.smtp.port", "587");

        Session session = Session.getInstance(props,
          new javax.mail.Authenticator() {
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(username, password);
            }
        });

        try {

            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress("mrupperman@gmail.com"));
            message.setRecipients(Message.RecipientType.TO,
                InternetAddress.parse("bupperman@bcicontrols.com, mrupperman@gmail.com"));
            message.setSubject("Monthly Niagara Meter Report  " + dateFormat.format(today));
            //message.setText();

            String nf = "E:\\Niagara\\Niagara-3.7.106\\Meter Reporting\\Monthly Reports\\" + dateFormat.format(today) + "EletricMeterReport.csv";
            File f = new File(nf);
            MimeBodyPart attachmentPart = new MimeBodyPart();
            attachmentPart.attachFile(f);

            MimeBodyPart textPart = new MimeBodyPart();
            textPart.setText( "Dear Ohio University Facility Staff,"
                + "\n\n Kindly find attached the monthly energy meter report!"
                + "\n\n\n Thanks, \n\n\n Building Control Integrators", "utf-8" );
                
            //MimeBodyPart htmlPart = new MimeBodyPart();
            //htmlPart.setContent( html, "text/html; charset=utf-8" );

            multipart.addBodyPart(attachmentPart);
            multipart.addBodyPart(textPart);
                        
            message.setContent(multipart);

            Transport.send(message);

            System.out.println(dateTimeFormat.format(today) + "    Meter Report Generated and Successfully Sent!");

        } catch (MessagingException e) {
            throw new RuntimeException(e);
        }
}

public void onStop() throws Exception
{
  // shutdown code here
}

