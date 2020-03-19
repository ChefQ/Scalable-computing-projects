var AWS = require("aws-sdk");

AWS.config.update({
  region: "ca-central-1",
  endpoint: "https://dynamodb.ca-central-1.amazonaws.com",
  accessKeyId: "",
  secretAccessKey: ""
});

var docClient = new AWS.DynamoDB.DocumentClient();

var table = "place";

/* var params = {
    TableName: table,
    Limit: 1
};

docClient.scan(params, function(err, data) {
    if(err) {
        console.error("Unable to read item. Error JSON:", JSON.stringify(err, null, 2));
    } else {
        if(data.Count == 0) {
            console.log("False");
        }
        else{
            console.log("True");
            console.log(JSON.stringify(data));
        }
    }
}); */



var params = {
    TableName: table,
    Item:{
        "coordinate": "(5,0)",
        "test": "hey!"
    }
};

// docClient.get(params, function(err, data) {
//     if (err) {
//         console.error("Unable to read item. Error JSON:", JSON.stringify(err, null, 2));
//     } else {
//         console.log("GetItem succeeded:", JSON.stringify(data, null, 2));
//     }
// });

docClient.put(params, function(err,data) {
    if(err) {
        console.error("Unable to read item. Error JSON:", JSON.stringify(err, null, 2));
    } else {
        console.log("PutItem succeeded:", JSON.stringify(data, null, 2));
    }
});