<%@ page language="java" contentType="text/html; charset=UTF-8"
    pageEncoding="UTF-8"%>
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
<title>Insert title here</title>
<style>
#specs {
    font-family: "Trebuchet MS", Arial, Helvetica, sans-serif;
    border-collapse: collapse;
    width: 95%;
}
#specs td, #customers th {
    border: 0px solid #ddd;
    padding: 8px;
}

#specs tr:nth-child(even){background-color: #f2f2f2;}

#specs tr:hover {background-color: #ddd;}

#specs th {
    padding-top: 12px;
    padding-left: 12px;
    padding-bottom: 12px;
    text-align: left;
    background-color: #4e7F50;
    color: white;
}
</style>
<script>

   var fragmentString = location.hash.substr(1);
   var fragment = {};
   var fragmentItemStrings = fragmentString.split('&');
   for (var i in fragmentItemStrings) {
     var fragmentItem = fragmentItemStrings[i].split('=');
     if (fragmentItem.length !== 2) {
       continue;
     }
     fragment[fragmentItem[0]] = fragmentItem[1];
     //alert(fragment['id_token']);
     var val=fragment['id_token'];
     if(val){
    	 myRequest= new XMLHttpRequest();
    	 myRequest.onreadystatechange = function() {
    	        if (this.readyState == 4 && this.status == 200) {
    	            console.log(this.responseText);
    	            console.log(myRequest.responseURL);
    	            document.body.innerHTML=this.responseText;
    	        }
    	    };
         myRequest.open("GET", "http://localhost:8080/auth/details", true);
    	 
             
         
     myRequest.setRequestHeader("Authorization"," Bearer "+val);
     history.pushState(null, "details", '/auth/details');    
     myRequest.send();
     }
     
   }
</script>
</head>
<body>

</body>
</html>