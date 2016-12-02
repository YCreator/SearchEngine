<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html>
<head><title>59图搜款</title></head>
<style type="text/css" >
	.myclass{
		margin: 9px 0;
	}
</style>
<body>
    <h1>欢迎使用搜图工具</h1>
    
    <form action="search" method="post">
    	<input type="text" id="text" name="url" size="50" /><br />
    	<div class="myclass">
    	<input type="radio" name="method" value="CEDD" />CEDD
    	<input type="radio" name="method" value="FCTH" />FCTH
    	<input type="radio" name="method" value="AutoColorCorrelogram" />AutoColorCorrelogram
    	</div><br/>
    	<input type="submit" value="搜图" id="submit"/>
    </form>
</body>
</html>