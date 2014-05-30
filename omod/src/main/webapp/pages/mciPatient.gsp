<html>
    <head>
        <title>Master Client Index Search Page</title>
        <link rel="shortcut icon" type="image/ico" href="/${ ui.contextPath() }/images/openmrs-favicon.ico"/>
        <link rel="icon" type="image/png\" href="/${ ui.contextPath() }/images/openmrs-favicon.png"/>

        <%
            ui.includeJavascript("uicommons", "jquery-1.8.3.min.js", Integer.MAX_VALUE)
            ui.includeCss("uicommons", "styleguide/jquery-ui-1.9.2.custom.min.css", Integer.MAX_VALUE - 10)
            ui.includeJavascript("uicommons", "emr.js", Integer.MAX_VALUE - 15)
            ui.includeJavascript("uicommons", "jquery.toastmessage.js", Integer.MAX_VALUE - 20)
            ui.includeCss("uicommons", "styleguide/jquery.toastmessage.css", Integer.MAX_VALUE - 20)
        %>

        ${ ui.resourceLinks() }
    </head>
    <body>
        <script type="text/javascript">
            var OPENMRS_CONTEXT_PATH = '${ ui.contextPath() }';
            window.translations = window.translations || {};
        </script>

        <ul id="breadcrumbs"></ul>

        <div id="body-wrapper">
            ${ ui.includeFragment("uicommons", "infoAndErrorMessage") }
            <div id="content" class="container" align="center>
                <p>Search Patient in National Registry</p>
                <select id="idType">
                  <option value="nid">National ID</option>
                  <option value="hid">Health ID</option>
                </select>
                <input id="patientId" type="text" name="patientId" value="">
            </div>
        </div>

        <script id="breadcrumb-template" type="text/template">
            <li>
                {{ if (!first) { }}
                <i class="icon-chevron-right link"></i>
                {{ } }}
                {{ if (!last && breadcrumb.link) { }}
                <a href="{{= breadcrumb.link }}">
                {{ } }}
                {{ if (breadcrumb.icon) { }}
                <i class="{{= breadcrumb.icon }} small"></i>
                {{ } }}
                {{ if (breadcrumb.label) { }}
                {{= breadcrumb.label }}
                {{ } }}
                {{ if (!last && breadcrumb.link) { }}
                </a>
                {{ } }}
            </li>
        </script>

        <script type="text/javascript">
            jq = jQuery;
            jq(function() {
                emr.updateBreadcrumbs();
            });

            // global error handler
            jq(document).ajaxError(function(event, jqxhr) {
                emr.redirectOnAuthenticationFailure(jqxhr);
            });
            jq(function() {
                 jq('#patientId').keypress(function(e) {
                    if(e.which == 13) {
                        console.log(jq( "#idType" ).val());
                        console.log(jq( "#patientId" ).val());
                    }
                 });
            });
        </script>

    </body>
</html>



