# Websocket

Market data feeds and notifications can be accessed via STOMP over websocket.

STOMP (or Simple Text Oriented Message Protocol) is a simple text-based protocol allowing a client to subscribe to various topics.

All our topics are authenticated.

**Authorization** header is required on the first CONNECT request.

For details on bearer token, please refer to the authentication section.

#### **WEBSOCKET ENDPOINT URL**
```json 
wss://ws.finxflo.com/stomp
  ``` 
#### CONNECT Request
```json 
CONNECT
Authorization:Bearer eyJraWQiOiJKb3BaYTl6SHpLT0hISGxueDlwSUJLMzYxTG1ITDlTeG9mT1hCS1FIMHRRIiwiYWxnIjoiUlMyNTYifQ.eyJ2ZXIiOjEsImp0aSI6IkFULnBpTGtUUWxXMVpvZ2VYZEQ0c3ZuY2ZuXzJILW8yMHhwdXd3TU1tQk9MR28iLCJpc3MiOiJodHRwczovL2ZpbnhmbG8ub2t0YS5jb20vb2F1dGgyL2RlZmF1bHQiLCJhdWQiOiJhcGk6Ly9kZWZhdWx0IiwiaWF0IjoxNjIzODA3NzYxLCJleHAiOjE2MjM4MTEzNjEsImNpZCI6IjBvYW9kYXNzYUNYU1g0M2ExNWQ2IiwidWlkIjoiMDB1MTBpZTlvNW1sQWpYWWo1ZDciLCJzY3AiOlsiZW1haWwiLCJvcGVuaWQiXSwic3ViIjoic2FuamF5a3VtYXJzaW5naDkyMDFAZ21haWwuY29tIn0.DfjEVfG-S9ssvxwPQ-Tdv_okKN0v2XyRXV9ERap7AOUXKaWv1gWLmIlSCs1GzbylwDNbJOfj0_3WABAO690fudN3GnVxuO0tkRaAMiSCsMjyttK7AMAeTe5TmD-GpO0GEpeezVJz5Q9IMcel-vuF93238NA3cHgbMq1RfM7oeZtgN5jBz7PygtFSaJmfJbYHqORpg7N9P3sTSJGezaFk_oZti9bUHDYa9to8SZQQGAI01sRhsf-cGPmEwIPNEHW8HU8vcwXgrxAs9aDRT5nRMuJsZkRUWAFCP92M36yYNwh11iVFSCngxi4r1a0SLWx3qMBYBvbbsq_SBb6YPymolA
accept-version:1.2,1.1,1.0
heart-beat:10000,10000
```

#### CONNECTED Response
```json 
CONNECTED
session:session-uk-AcjZ78gKrRWHuq55hzQ
heart-beat:10000,10000
version:1.2
user-name:1acd0bfd-ef03-41f6-b64e-4e716d77ab46
```

## Available Topics

### Order Book
>/topic/{market}.depth

{market} can be any of the markets returned by the /markets REST API.

On subscription, we will send you a snapshot of our order book.
Subsequently, we will push you all the changes.

#### Response
```json 
[
  {
    "side": "ASK",
    "price": 0.06305,
    "quantity": 1.2,
    "timestamp": 1623808114541
  },
  {
    "side": "BID",
    "price": 0.06295,
    "quantity": 2.3,
    "timestamp": 1623808114544
  }
]
  ``` 

### Orders
>/user/topic/orders

On subscription, we will send a list of all your open orders.

#### First response listing all your open orders
```json 
[
  {
    "id": "b89080ad-88c6-42a1-aff8-412623dd4356",
    "date": "2021-05-28T08:12:44.793Z",
    "market": "ETH_BTC",
    "side": "BID",
    "status": "PENDING_NEW",
    "type": "LIMIT",
    "quantity": 0.1,
    "price": 0.1,
    "total": null,
    "fills": [],
    "averagePrice": null,
    "fillQuantity": 0,
    "fillPercent": 0
  }
]
```

Subsequently, we will push you updates on individual orders.

#### order updates
```json 
{
  "id": "d155dec3-5b65-4274-97d3-96a3a1906091",
  "date": "2021-06-14T02:38:01.847Z",
  "market": "ETH_BTC",
  "side": "BID",
  "status": "SETTLED",
  "type": "MARKET",
  "quantity": 0.01,
  "price": null,
  "total": 0.15569,
  "fills": [
    {
      "id": "c935f548-5dd2-4f6f-be93-af81eef77895",
      "date": "2021-06-14T02:38:28.615641Z",
      "quantity": 0.01,
      "price": 0.06423
    }
  ],
  "averagePrice": 0.06423,
  "fillQuantity": 0.01,
  "fillPercent": 100
}
```

### Fills
>/user/topic/fills

#### Response
```json 
{
  "id": "27e7f34c-bcc3-41d8-babe-caee5f681c08",
  "orderId": "85c47080-ce68-11eb-b8bc-0242ac130003"
  "date": "2019-08-24T14:15:22Z",
  "quantity": 1,
  "price": 1.3343
}
``` 

# Signature
## Rest APIs

Request signature is required when user is using api keys to authenticate the request. You can generate key pair from our trading
platform and save the private key with yourself for generating the request signature before calling any API.

#### Request Signing Example
```json 
Request body
{
  "side": "ASK",
  "quantity": 0.01,
  "type": "MARKET",
  "market": "ETH_BTC"
}

Query param
test=abc

// Signature string before signing with the private key
Signature{body='{
    "side": "ASK",
    "quantity": 0.01,
    "type": "MARKET",
    "market": "ETH_BTC"
}', queryParam='test=abc'}

After signing with private key signature needs to be sent in header like this. x-key is the key id that user get when they generate the key.
x-signature = WQAdrEe9DLnLYh6qcQf5f/RKJk+n8C0K+YtwikNaXipeADdv5sGt53LUMPjRvECJYSrKtURZlzR/srfkFVInmMrvW6WWvZdRLor6hUHWs73pG1MEooiLMY5GzU/z34Pf/SEb8LanzMhSkVzLhgTdvXMi7/BWTlv5r1Jne5GCz214kbWEsLzB2o5QvgWLL/09iA26b7QLoOeN/5st+euOc31Ks779qKjJEDBjI31BMxwnBHcZaTB4plNE16HsR9O6KwnCH5ytaV+26TjmhVHFbJk0C6BOtc58dhvpA5DYazE1eWFUTEml32/0F3l57NW7qWmN2CEs9647MlN/iBBtuA==
x-key = 919fc5c2-a798-447d-822a-e0e62069c2c1

``` 

#### Java Code to generate signature


``` 
public class RequestSignature {
  private final String body;
  private final String queryParam;

  public RequestSignature(String body, String queryString) {
    this.body = StringUtils.hasText(body) ? body : "";
    this.queryParam = StringUtils.hasText(queryString) ? queryString : "";
  }

  public String sign(String privateKey)
          throws NoSuchAlgorithmException, InvalidKeyException, SignatureException,
          InvalidKeySpecException {
    var privateSignature = java.security.Signature.getInstance("SHA256withRSA");
    privateSignature.initSign(RSAUtil.getPrivateKey(privateKey));
    privateSignature.update(this.create().getBytes(StandardCharsets.UTF_8));
    var signature = privateSignature.sign();
    return Base64.getEncoder().encodeToString(signature);
  }

  public String create() {
    return "Signature{" + "body='" + body + '\'' + ", queryParam='" + queryParam + '\'' + '}';
  }
  
  public static void main(String[] args) {
    var signatureString = new RequestSignature("{\n" +
            "  \"side\": \"ASK\",\n" +
            "  \"quantity\": 0.01,\n" +
            "  \"type\": \"MARKET\",\n" +
            "  \"market\": \"ETH_BTC\"\n" +
            "}", "");
    var signature = signatureString.create();
    signatureString.sign("MIIBOQIBAAJAWrJ47dDUrsORiu4q5A/Svl+uyAvWijTQZhLv+8Ysry/uYcnqQPXQG+UVVL/5gnqbLp+EtGQqsCo/5smCOiqwnwIDAQABAkABkhu0CqzURgDMRimp/3gn4eJWBpZ1mEqPqf5L/vehJQ8MmY5+vJG78Gpv7XF/wB0jjKjDXRfYAOmlDIa1fIk5AiEAqxkr9mZKtOLnkDiT0y3wDNUe7O4quLOvhyZXVGxBRYUCIQCHs9esEBwARHWnUPpusDO+d8WKdi+zJgkfnIt+xjAU0wIgOkzCcRwb2pTyaG8O18dwYz7/YaYpwnPfnIKRAUA94W0CIAKysawLGfNraQdtlb0TpcO4r+XD2cjvDaliPGfF2vjfAiEAkORV7AIVImZNU48zbMGoQ0kZ3Umyp4ABM8kNQvEfZYs=");
  }
}

``` 

## Websocket

You can use api keys to connect to websocket by sending x-key and x-signature in tha header instead of Authorization header.
x-key is the key id that you will get when you generate api keys.
x-signature is signed base64 string of key id from private key.