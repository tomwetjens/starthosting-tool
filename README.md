# starthosting-tool
Tool for Dynamic DNS with StartHosting 

## Usage
```
java -jar starthosting-tool.jar dynamic -user 12345678 -password mypassword -domain mydomain.com -type A -url http://icanhazip.com/ -interval 600000
```

This will update the A records of mydomain.com with the actual public IP address and checks the public IP address every 10 minutes.
