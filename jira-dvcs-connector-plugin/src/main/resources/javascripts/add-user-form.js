define("dvcs/add-user-form", function () {
    "use strict";

    var AddUserForm = function($applications, $form, $softwareAccess) {
        this.$applications = $applications;
        this.$form = $form;
        this.$softwareAccess = $softwareAccess;
    };

    AddUserForm.prototype = {
        isSoftwareApplicationSelected: function() {
            return this.$softwareAccess.attr('checked');
        },

        getSelectedApplicationCount: function() {
            return this.$applications.filter(':checked').length;
        },

        onApplicationSelectionChange: function(callback) {
            this.$applications.on('change', callback);
        },

        onSubmit: function(callback) {
            this.$form.on('submit', callback);
        }
    };

    return AddUserForm;
});