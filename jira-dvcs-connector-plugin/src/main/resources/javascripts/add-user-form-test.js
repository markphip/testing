AJS.test.require("com.atlassian.jira.plugins.jira-bitbucket-connector-plugin:add-user-bitbucket-access-components", function () {
    "use strict";

    require([
        "dvcs/add-user-form"
    ], function(
        AddUserForm
    ) {
        module('Add user form', {
            setup: function() {
                this.sandbox = sinon.sandbox.create();

                this.applications = {
                    filter: this.sandbox.stub(),
                    on: this.sandbox.stub()
                }

                this.form = {
                    on: this.sandbox.stub()
                }

                this.softwareAccess = {
                    attr: this.sandbox.stub()
                }

                this.addUserForm = new AddUserForm(this.applications, this.form, this.softwareAccess);
            },

            teardown: function() {
                this.sandbox.restore();
            }
        });

        test("Should return true when isSoftwareApplicationSelected when Software application is selected", function() {
            this.softwareAccess.attr.withArgs("checked").returns(true);

            equal(this.addUserForm.isSoftwareApplicationSelected(), true);
        });

        test("Should return false when isSoftwareApplicationSelected when Software application is not selected", function() {
            this.softwareAccess.attr.withArgs("checked").returns(false);

            equal(this.addUserForm.isSoftwareApplicationSelected(), false);
        });
        
        test("Should return 0 when getSelectedApplicationCount is invoked and no applications are selected", function() {
            this.applications.filter.withArgs(":checked").returns([]);

            equal(this.addUserForm.getSelectedApplicationCount(), 0);
        });

        test("Should return number of applications selected when getSelectedApplicationCount is invoked", function() {
            this.applications.filter.withArgs(":checked").returns([{}, {}, {}]);

            equal(this.addUserForm.getSelectedApplicationCount(), 3);
        });

        test("Should register a change event listener when onApplicationSelectionChange is invoked", function() {
            var callback = function() {};

            this.addUserForm.onApplicationSelectionChange(callback);

            sinon.assert.calledWithExactly(this.applications.on, "change", callback);
        });

        test("Should register a submit event listener when onSubmit is invoked", function() {
            var callback = function() {};

            this.addUserForm.onSubmit(callback);

            sinon.assert.calledWithExactly(this.form.on, "submit", callback);
        });
    })
});