<html>
    <head>
        <title>Master Client Index Search Page</title>
        <link rel="shortcut icon" type="image/ico" href="/${ ui.contextPath() }/images/openmrs-favicon.ico"/>
        <link rel="icon" type="image/png\" href="/${ ui.contextPath() }/images/openmrs-favicon.png"/>

        <%
            ui.includeJavascript("uicommons", "jquery-1.8.3.min.js", Integer.MAX_VALUE)
            ui.includeCss("uicommons", "styleguide/jquery-ui-1.9.2.custom.min.css", Integer.MAX_VALUE - 10)
            ui.includeJavascript("uicommons", "jquery.toastmessage.js", Integer.MAX_VALUE - 20)
            ui.includeJavascript("bdshrclient", "mustache.js", Integer.MAX_VALUE - 30)
            ui.includeCss("uicommons", "styleguide/jquery.toastmessage.css", Integer.MAX_VALUE - 20)
        %>

        <style>
            ul.mciPatientInfo { list-style-type:none; }
        </style>

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
            <div id="content" class="container" align="center">
                <p>Search Patient in National Registry</p>
                <select id="idType">
                  <option value="nid">National ID</option>
                  <option value="hid">Health ID</option>
                </select>
                <input id="patientId" type="text" name="patientId" value="">

                <br/>
                <br/>

                <div id="mciPatients">
                </div>

            </div>
        </div>

        <script id="mciPatientTmpl" type="x-tmpl-mustache">
            <ul class="mciPatientInfo">
                <li>
                    {{ fullName }}, {{ gender }}
                </li>
                <li>
                    Address: District - {{ address.districtId }}, Upazilla - {{ address.upazillaId }}
                </li>
            </ul>
        </script>

        <script type="text/javascript">
            jq = jQuery;
            //jq(function() {
            //  emr.updateBreadcrumbs();
            //});

            // global error handler
            // jq(document).ajaxError(function(event, jqxhr) {
            //    emr.redirectOnAuthenticationFailure(jqxhr);
            // });

            jq(function(){
                jq('#patientId').keypress(function(e) {
                   if(e.which == 13) {
                       searchPatient();
                    }
                });
            });

            function searchPatient() {
                var idType = jq( "#idType" ).val();
                var patientId = jq( "#patientId" ).val();
                jq.ajax({
                   type: "GET",
                   url: "/openmrs/ws/mci/search?" + idType + "=" + patientId,
                   dataType: "json"
                }).done(function( responseData ) {
                   renderMciPatient(responseData);
                }).fail(function(error) {
                   alert( "error occurred : " + error);
                });
            }

            function renderMciPatient(patient) {
                if (!patient) {
                    jq("#mciPatients").html("No patient was found in National Registry");
                    return;
                }
                var template = jq('#mciPatientTmpl').html();
                Mustache.parse(template);
                var rendered = Mustache.render(template, patient);
                jq('#mciPatients').html(rendered);
            }
        </script>

    </body>
</html>



