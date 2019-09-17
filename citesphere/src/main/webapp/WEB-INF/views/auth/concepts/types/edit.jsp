<%@ page pageEncoding="UTF-8" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<%@ taglib prefix="sec" uri="http://www.springframework.org/security/tags" %>
<%@ taglib uri = "http://java.sun.com/jsp/jstl/functions" prefix = "fn" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form"%>


<ol class="breadcrumb">
  <li><a href="<c:url value="/" />">Home</a></li>
  <li><a href="<c:url value="/auth/concepts/list" />">Concept Types</a></li>
  <li class="active">${form.name}</li>
</ol>
<h2>
	Edit Concept Type: ${form.name}	  
</h2>

<c:url value="/auth/concepts/types/${typeId}/edit" var="processingUrl" />
<form:form action="${processingUrl}" modelAttribute="form" method="POST" id="editForm">

<label>Name:</label>
<form:input path="name" type="text" class="form-control" placeholder="Name"></form:input>
<p class="text-danger"><form:errors path="name"/></p>

<label>Description:</label>
<form:input path="description" type="text" class="form-control" placeholder="Description"></form:input>
<p></p>
<label>URI:</label>
<form:input path="uri" type="text" class="form-control" placeholder="URI"></form:input>
<br>
<button id="submitForm" class="btn btn-primary" type="submit"><i class="far fa-save"></i> &nbsp;Save</button>
<a href="<c:url value="/auth/concepts/types/list" />" class="btn btn-default">
		<i class="fa fa-times"></i>&nbsp;Cancel 
</a>
</form:form>