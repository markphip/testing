define("dvcs/bitbucket-access-controller", [
    "underscore"
], function (
    _
) {
    "use strict";

    var BitbucketAccessController = function(addUserForm, bitbucketAccess, bitbucketInviteToGroups) {
        this.addUserForm = addUserForm;
        this.bitbucketAccess = bitbucketAccess;
        this.bitbucketGroupsInputField = '<input type="hidden" name="dvcs_org_selector" value="' + bitbucketInviteToGroups + '" />';
    };

    BitbucketAccessController.prototype = {
        start: function() {
            this.establishInitialState();
            this.registerListeners();
        },

        establishInitialState: function() {
            if (this.addUserForm.isSoftwareApplicationSelected()) {
                this.bitbucketAccess.select();
            }

            if (this.addUserForm.getSelectedApplicationCount() === 0 ) {
                this.bitbucketAccess.disable();
            }
        },

        registerListeners: function() {
            this.addUserForm.onApplicationSelectionChange(_.bind(this.onApplicationSelectionChange, this));
            this.addUserForm.onSubmit(_.bind(this.onFormSubmit, this));
        },

        onApplicationSelectionChange: function() {
            var selectedApplicationCount = this.addUserForm.getSelectedApplicationCount();

            if (selectedApplicationCount === 0 && this.bitbucketAccess.isEnabled()) {
                this.bitbucketAccess.disable();
            }

            if (selectedApplicationCount !== 0 && !this.bitbucketAccess.isEnabled()) {
                this.bitbucketAccess.enable();
            }
        },

        onFormSubmit: function() {
            if (this.bitbucketAccess.isSelected()) {
                this.addUserForm.append(this.bitbucketGroupsInputField);
            }
        }
    };

    return BitbucketAccessController;
});