Interbank Payment Service SDK For Java
======================================

##ოპერაციები
- ფინანსური შეტყობინების გაგზავნა
- გზავნილის გაუქმება
- გაგზავნილი შეტყობინებების დათვალიერება
- მიღებული გზავნილების დათვალიერება
- მიღებული გზავნილის დადასტურება
- მიღებული გზავნილის უარყოფა

##ApiClient კლასი
კლასი `ApiClient` წაროადგენს **IPS API**-სთან სამუშაო ძირითად კომპონენტს. კლასის ეგზემპლიარის შესაქმენლად საჭიროა შემდეგი პარამეტრების განსაზღვრა.

- `apiAddress` - **API**-ს მისამართი (`URL`).
- `participantId` - მონაწილის იდენთიფიკატორი. ბანკის შემთხვევაში **BIC**
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
  -  `1` საგადახდო დავალება
  - `999` თავისუფალი ტექსტი

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
X509Certificate receiverCert =... // ინიციალიზაციის ლოგიკა გამოტოვებულია 
apiClient.send(receiverCert,
      "BNKBGE22",
      "ref0001",
      new Short((short) 1),
      new DateTime(Calendar.getInstance().getTime()),
      "This is sample message from IPS SDK For Java",
      new BigDecimal(17),
      "GEL");                
```