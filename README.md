# kareer-ms-user

## Generate a salt

```shell
echo $(htpasswd -bnBC 12 "" password | tr -d ':\n' | sed 's/$2y/$2a/')
```
This will generate a BCrypt salt of cost 12 based of the value "password". You can increase/decrease cost or change the value to your liking. More info [here](https://unix.stackexchange.com/a/419855).
