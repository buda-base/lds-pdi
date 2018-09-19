package io.bdrc.ldspdi.annotations;

import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;

/**
 The standard Friend of a Friend mode
 */

public class Foaf {
    /**
     * The namespace of the vocabulary as a string
     */
    public static final String NS = "http://xmlns.com/foaf/0.1/";

    /** returns the URI for this schema
        @return the URI for this schema
     */
    public static String getURI()
    { return NS; }

    protected static final Resource resource( String local )
    { return ResourceFactory.createResource( NS + local ); }

    protected static final Property property( String local )
    { return ResourceFactory.createProperty( NS, local ); }

    public final static Resource Agent = resource( "Agent" );

    public final static Resource Document = resource( "Document" );

    public final static Resource Group = resource( "Group" );

    public final static Resource Image = resource( "Image" );

    public final static Resource LabelProperty = resource( "LabelProperty" );

    public final static Resource OnlineAccount = resource( "OnlineAccount" );

    public final static Resource OnlineChatAccount = resource( "OnlineChatAccount" );

    public final static Resource OnlineEcommerceAccount = resource( "OnlineEcommerceAccount" );

    public final static Resource OnlineGamingAccount = resource( "OnlineGamingAccount" );

    public final static Resource Organization = resource( "Organization" );

    public final static Resource Person = resource( "Person" );

    public final static Resource PersonalProfileDocument = resource( "PersonalProfileDocument" );

    public final static Resource Project = resource( "Project" );

    public final static Property account = property( "account" );

    public final static Property accountName = property( "accountName " );

    public final static Property accountServiceHomepage = property( "accountServiceHomepage" );

    public final static Property aimChatID = property( "aimChatID" );

    public final static Property based_near = property( "based_near" );

    public final static Property currentProject = property( "currentProject" );

    public final static Property depiction = property( "depiction" );

    public final static Property depicts = property( "depicts" );

    public final static Property familyName = property( "familyName" );

    public final static Property firstName = property( "firstName" );

    public final static Property focus = property( "focus" );

    public final static Property gender = property( "gender" );

    public final static Property givenName = property( "givenName" );

    public final static Property icqChatID = property( "icqChatID" );

    public final static Property img = property( "img" );

    public final static Property interest = property( "interest" );

    public final static Property jabberID = property( "jabberID" );

    public final static Property lastName = property( "lastName" );

    public final static Property logo = property( "logo" );

    public final static Property mbox_sha1sum = property( "mbox_sha1sum" );

    public final static Property msnChatID = property( "msnChatID" );

    public final static Property myersBriggs = property( "myersBriggs" );

    public final static Property name = property( "name" );

    public final static Property nick = property( "nick" );

    public final static Property openid = property( "openid" );

    public final static Property pastProject = property( "pastProject" );

    public final static Property phone = property( "phone" );

    public final static Property plan = property( "plan" );

    public final static Property publications = property( "publications" );

    public final static Property schoolHomepage = property( "schoolHomepage" );

    public final static Property skypeID = property( "skypeID" );

    public final static Property thumbnail = property( "thumbnail" );

    public final static Property tipjar = property( "tipjar" );

    public final static Property title = property( "title" );

    public final static Property topic = property( "topic" );

    public final static Property topic_interest = property( "topic_interest" );

    public final static Property workInfoHomepage = property( "workInfoHomepage" );

    public final static Property workplaceHomepage = property( "workplaceHomepage" );

    public final static Property yahooChatID = property( "yahooChatID" );

    public final static Property age = property( "age" );

    public final static Property birthday = property( "birthday" );

    public final static Property membershipClass = property( "membershipClass" );

    public final static Property sha1 = property( "sha1" );

    public final static Property status = property( "status" );

    public final static Property dnaChecksum = property( "dnaChecksum" );

    public final static Property family_name = property( "family_name" );

    public final static Property fundedBy = property( "fundedBy" );

    public final static Property geekcode = property( "geekcode" );

    public final static Property givenname = property( "givenname" );

    public final static Property holdsAccount = property( "holdsAccount" );

    public final static Property surname = property( "surname" );

    public final static Property theme = property( "theme" );
}
