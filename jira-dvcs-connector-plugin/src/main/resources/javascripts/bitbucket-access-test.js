AJS.test.require("com.atlassian.jira.plugins.jira-bitbucket-connector-plugin:add-user-bitbucket-access-components", function () {
    "use strict";

    require([
        "dvcs/bitbucket-access"
    ], function(
        BitbucketAccess
    ) {
        module('Bitbucket access', {
            setup: function() {
                this.sandbox = sinon.sandbox.create();

                this.bitbucketAccessOption = {
                    attr: this.sandbox.stub()
                };

                this.bitbucketInfoIcon = {
                    addClass: this.sandbox.stub(),
                    removeClass: this.sandbox.stub()
                };

                this.bitbucketAccess = new BitbucketAccess(this.bitbucketAccessOption, this.bitbucketInfoIcon);
            },

            teardown: function() {
                this.sandbox.restore();
            }
        });
        
        test("Should enable checkbox and hide info icon when access is enabled", function() {
            this.bitbucketAccess.enable();

            sinon.assert.calledWithExactly(this.bitbucketAccessOption.attr, "disabled", false);
            sinon.assert.calledWithExactly(this.bitbucketInfoIcon.addClass, "hidden");
        });

        test("Should clear selection, disable checkbox and show info icon when access is disabled", function() {
            this.bitbucketAccess.disable();

            sinon.assert.calledWithExactly(this.bitbucketAccessOption.attr, "checked", false);
            sinon.assert.calledWithExactly(this.bitbucketAccessOption.attr, "disabled", true);
            sinon.assert.calledWithExactly(this.bitbucketInfoIcon.removeClass, "hidden");
        });
        
        test("Should check the checkbox when select is invoked", function() {
            this.bitbucketAccess.select();

            sinon.assert.calledWithExactly(this.bitbucketAccessOption.attr, "checked", true);
        });

        test("Should uncheck the checkbox when deselect is invoked", function() {
            this.bitbucketAccess.deselect();

            sinon.assert.calledWithExactly(this.bitbucketAccessOption.attr, "checked", false);
        });
        
        test("Should return true when isEnabled is invoked and checkbox is enabled", function() {
            this.bitbucketAccessOption.attr.withArgs("disabled").returns(false);

            equal(this.bitbucketAccess.isEnabled(), true);
        });

        test("Should return false when isEnabled is invoked and checkbox is disabled", function() {
            this.bitbucketAccessOption.attr.withArgs("disabled").returns(true);

            equal(this.bitbucketAccess.isEnabled(), false);
        });

        test("Should return true when isSelected is invoked and checkbox is checked", function() {
            this.bitbucketAccessOption.attr.withArgs("checked").returns(true);

            equal(this.bitbucketAccess.isSelected(), true);
        });

        test("Should return false when isSelected is invoked and checkbox is unchecked", function() {
            this.bitbucketAccessOption.attr.withArgs("checked").returns(false);

            equal(this.bitbucketAccess.isSelected(), false);
        });
    })
});