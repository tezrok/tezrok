# Tezrok - Java Backend Generator

## Usage

```bash
git clone https://github.com/tezrok/tezrok.git
cd tezrok

mvn clean package -DskipTests

./tezrok.sh ./samples/tezrok-hello.json
# the project will be generate in ./samples/tezrok-hello/
```

## Configuration
The configuration file is a JSON file that defines the project structure, dependencies, and other settings. Here is an example configuration:

```json
{
  "name": "hello-app",
  "productName": "Hello",
  "version": "1.0-SNAPSHOT",
  "description": "Simple hello application",
  "package": "com.example.hello",
  "author": "",
  "modules": [
    {
      "name": "hello",
      "description": "Simple web application",
      "type": "Spring",
      "schema": {
        "schemaName": "hl",
        "entities": [
          {
            "name": "Customer",
            "description": "Customer entity",
            "createdAt": true,
            "updatedAt": true,
            "customRepository": true,
            "customService": true,
            "fields": [
              {
                "name": "id",
                "type": "Long",
                "serial": true,
                "primary": true
              },
              {
                "name": "fullName",
                "type": "String",
                "description": "Description of subject",
                "required": true,
                "maxLength": 150
              },
              {
                "name": "lang",
                "type": "String",
                "description": "Language code",
                "required": true,
                "maxLength": 2
              }
            ]
          },
          {
            "name": "Order",
            "description": "Order entity",
            "fields": [
              {
                "name": "id",
                "type": "Long",
                "primary": true,
                "serial": true
              },
              {
                "name": "customer",
                "type": "Customer",
                "required": true,
                "relation": "ManyToOne"
              }
            ]
          }
        ]
      },
      "auth": {
        "type": "simple",
        "stdInit": true
      }
    }
  ]
}
```
## License
This project is licensed under the Apache 2.0. See the [LICENSE](LICENSE) file for details