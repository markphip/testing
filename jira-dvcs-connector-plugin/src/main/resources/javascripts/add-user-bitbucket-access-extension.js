AJS.$(function() {
    require([
        'dvcs/add-user-form',
        'dvcs/bitbucket-access',
        'dvcs/bitbucket-access-controller',
        'jquery',
        'aui/inline-dialog2' //Initialise the inlineDialog2. This is all that is required.
    ], function(
        AddUserForm,
        BitbucketAccess,
        BitbucketAccessContoller,
        $
    ) {
        var $addUserForm = $('#user-create');
        var $bitbucketAccessOption = $('#should-invite-user');
        var $bitbucketInfoIcon = $('.bitbucket-access-field-group .checkbox .aui-iconfont-info');
        var $jiraApplications = $('.application-picker-applications input.checkbox.application');
        var $jiraSoftwareAccessOption = $('.checkbox.application-jira-software');

        var addUserForm = new AddUserForm($jiraApplications, $addUserForm, $jiraSoftwareAccessOption);
        var bitbucketAccess = new BitbucketAccess($bitbucketAccessOption, $bitbucketInfoIcon);
        var bitbucketAccessController = new BitbucketAccessContoller(addUserForm, bitbucketAccess);
        bitbucketAccessController.start();
    });
});
