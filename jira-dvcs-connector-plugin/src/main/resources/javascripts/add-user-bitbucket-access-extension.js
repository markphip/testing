AJS.$(function() {
    var $bitbucketAccessOption = AJS.$('#should-invite-user');
    var $bitbucketInfoIcon = AJS.$('.bitbucket-access-field-group .checkbox .aui-iconfont-info');
    var $jiraApplications = AJS.$('.application-picker-applications input.checkbox.application');
    var $jiraSoftwareAccessOption = AJS.$('.checkbox.application-jira-software');

    function selectBitbucketAccessOnLoadIfSoftwareIsSelected() {
        if($jiraSoftwareAccessOption.is(':checked')) {
            $bitbucketAccessOption.attr('checked', true);
        }
    }

    function atLeastOneJiraApplicationIsSelected() {
        return $jiraApplications.filter(':checked').length >= 1;
    }

    function disableBitbuckeAccessOnLoadIfNoJiraApplicationIsSelected() {
        if(!atLeastOneJiraApplicationIsSelected()) {
            disableBitbucketAccessOption();
        }
    }

    function enableBitbucketAccessOption() {
        $bitbucketInfoIcon.addClass('hidden');
        $bitbucketAccessOption.attr('disabled', false);
    }

    function disableBitbucketAccessOption() {
        $bitbucketInfoIcon.removeClass('hidden');
        $bitbucketAccessOption.attr('checked', false).attr('disabled', true);
    }

    function bitbucketAccessOptionIsDisabled() {
        return $bitbucketAccessOption.attr('disabled');
    }

    require(['aui/inline-dialog2']);
    selectBitbucketAccessOnLoadIfSoftwareIsSelected();
    disableBitbuckeAccessOnLoadIfNoJiraApplicationIsSelected();
    $jiraApplications.on('change', function() {
        var atLeastOneJiraAppIsSelected = atLeastOneJiraApplicationIsSelected();
        var bbOptionDisabled = bitbucketAccessOptionIsDisabled();

        if(atLeastOneJiraAppIsSelected && bbOptionDisabled) {
            enableBitbucketAccessOption();
        }

        if(!atLeastOneJiraAppIsSelected && !bbOptionDisabled) {
            disableBitbucketAccessOption();
        }
    });
});
