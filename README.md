Interbank Payment Service SDK for Java
======================================

##ოპერაციები
- ფინანსური შეტყობინების გაგზავნა
- გზავნილის გაუქმება
- გაგზავნილი შეტყობინებების დათვალიერება
- მიღებული გზავნილების დათვალიერება
- მიღებული გზავნილის დადასტურება
- მიღებული გზავნილის უარყოფა

##ApiClient კლასი
კლასი `ApiClient` წაროადგენს **IPS API**-სთან სამუშაო ძირითად კომპონენტს. 
კლასის ეგზემპლიარის შესაქმენლად საჭიროა შემდეგი პარამეტრების განსაზღვრა.

- `apiAddress` - **API**-ს მისამართი (`URL`).
- `participantId` - მონაწილის იდენტიფიკატორი. ბანკის შემთხვევაში **BIC**
- `privateKey` - მონაწილის დახურული გასაღები
- `certificate` - მონაწილის X509 სერთიფიკატი.

`ApiClient` კლასის ეგზემპლიარის შექმნის მაგალითი:

```java
public static ApiClient createApiClient(String apiAddress,
                                            String participantId,
                                            String pkcs12FilePath,
                                            String pkcs12Password)
            throws Exception {

        char[] password = pkcs12Password.toCharArray();
        KeyStore store = KeyStore.getInstance("pkcs12");
        FileInputStream f = null;

        try {
            f = new FileInputStream(pkcs12FilePath);
            store.load(f, password);
            String alias = store.aliases().nextElement();

            return new ApiClient(apiAddress,
                    participantId,
                    (PrivateKey) store.getKey(alias, password),
                    (X509Certificate) store.getCertificate(alias));


        } catch (Exception ex) {
            if (f != null) {
                f.close();
            }
            throw ex;
        }
    }
```
> მოცემულ მაგალითში, სიმარტივისთვის, დახურული გასაღების და სერტიფიკატის ჩატვირთვა ხორცილედება [PFX](https://en.wikipedia.org/wiki/PKCS_12) ფაილიდან.

## შეტყობინების გაგზავნა
შეტყობინების გასაგზავნად ვიყენებთ `ApiClient` კლასის `send` მეთოდს.

###პარამეტერები
- `receiverCertificate` - მიმღების სერთიფიკატი
- `receiver` - მიმღები
- `ref` - გზავნილის იდენტიფიკატორი, ენიჭება გამგზავნის მიერ
- `type` - გზავნილის ტიპის იდენტიფიკატორი.
  > დასაშვები მნიშვნელობები:
  -  `PAYMENT` საგადახდო დავალება
  - `TEXT` თავისუფალი ტექსტი

- `date` - თარიღი
- `content` - შეტყობინების ტექსტი
- `amount` - თანხა
- `ccy` ვალუტა

შეტყობინების გაგზავნის მაგალითი:

```java
ApiClient apiClient = createApiClient("https://ips.com",
                "BNKAGE22",
                "my.pfx",
                "my-pa$w0rd");
X509Certificate receiverCert; // ინიციალიზაციის ლოგიკა გამოტოვებულია
apiClient.send(receiverCert,
      "BNKBGE22",
      "ref0001",
      "PAYMENT",
      new DateTime(Calendar.getInstance().getTime()),
      "This is sample message from IPS SDK For Java",
      new BigDecimal(17),
      "GEL");
```

##გზავნილის გაუქმება

შესაძლებელია "დაუმუშავებელი" (არ არის გადაგზავნილი მიმღებთან) გზავნილის გაუქმება. ამისათვის გამოიყენეთ `ApiClient` კლასის `cancel` მეთიდი.

###პარამატრები
- `ref` - გზავნილის იდენტიფიკატორი
- `reason` - გაუქმების მიზეზი


```java
apiClient.cancel("ref0001", "TEst");
```

##გაგზავნილი შეტყობინებების (სტატუსების) დათვალიერება

გაგზავნილი შეტყობინებების სტატუსების დასათვალიერებლად გამოიყენეთ `ApiClient`-ის
`browseOutbox` მეთოდი. მეთოდი აბრუნებს გაგზავნილი შეტყობინებების სიას და `nextUri`-ს
შემდგომი გამოძახებისთვის, რადგან შემდგომ გამოძახებაზე მივიღოთ მხოლოდ ის გზავნილები
რომელთა სტატუსებიც შეიცვალა წინა გამოძახების შემდგომ.

###პარამეტრები
- `uri` - თუ ამ პარამეტრის მნიშვნელობაა `null` მაშინ დაბრუნდება ყველა გზავნილი, ხოლო თუ მეთოდს გადავცემთ წინა გამოძახებისას დაბრუნებულ `nextUri`-ს მაშინ მივიღებთ ყველა იმ გზავნილს რომელთა სტატუსიც შეიცვალა ბოლო გამოძახების შემდგომ.

```java
  MessageCollection outbox = apiClient.browseOutbox(null);
  String nextUri = outbox.getNext();

  System.out.println("-------------------------------First call---------------------------");

  for (Message message: outbox.getItems()){
      System.out.println(String.format("%s\t%s", message.getRef(), message.getState()));
  }

  System.out.println("-------------------------------Second call---------------------------");

  outbox = apiClient.browseOutbox(nextUri);

  for (Message message: outbox.getItems()){
      System.out.println(String.format("%s\t%s", message.getRef(), message.getState()));
  }
```


##მიღებული შეტყობინებების დათვალიერება

მიღებული შეტყობინებების დასათვალიერებლად გამოიყენეთ `ApiClient`-ის `browseInbox` მეთოდი.

###პარამტერები
- `all` თუ ამ პარამეტრის მნიშვნელობაა `true` მეთოდი დააბრუნებს ყველა მიღებულ შეტყობინებებს, წინააღმდეგ შემთხვევაში დაბრუნდება მხოლოდ "ახალი" შეტყობინებები.

##გზავნილის მიღება (დადასტურება)

გზავნილის მისაღებად გამოიყენეთ `ApiClient`-ის `complete` მეთოდი.

###პარამეტრები
- `messageId` - გზავნილის იდენტიფიკატორი (მინიჭებული სისტემის მიერ)

##გზავნილის უარყოფა

გზავნილის უარყოფისათვის გამოიყენეთ `ApiClient`-ის `reject` მეთოდი.

###პარამეტრები
- `messageId` - გზავნილის იდენტიფიკატორი (მინიჭებული სისტემის მიერ)
- `reason` - გზავნილის უარყოფის მიზეზი

ქვემოთ მოყვანილი მაგალითი სადაც ხდება ყველა იმ ახალი გზავნილის მიღება (დადასტურება) რომლის თანხა არ აღემატება 10000-ს.


```java
  MessageCollection inbox = apiClient.browseInbox(false);
  BigDecimal maxAmount = new BigDecimal(10000);

  for (Message message : inbox.getItems()){
      if (message.getAmount().compareTo(maxAmount) > 0  ){
          apiClient.reject(message.getId(), "Amount is out of range");
      }
      else{
          apiClient.complete(message.getId());                
      }
  }
```

