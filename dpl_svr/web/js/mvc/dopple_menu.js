/**
 *
 * Created By: Jeff Boehmer
 * Company: Ft. Collins Research
 * Website: www.ftcollinsresearch.org
 *          www.geofixated.org
 * Date: 10/3/13
 * Time: 8:52 AM
 */

Geof.menus = {
    main: [
      {
            name:'admin',
            strokeWidth:this.dftStrWidth,
            imageSrc:'Admin.png',
            title:'Admin',
            x:0,
            y:0,
            type:'menu'
        },{
            name:'manage',
            strokeWidth:this.dftStrWidth,
            imageSrc:'Gnome-Preferences-System-64.png',
            title:'Manange',
            x:1,
            y:0,
            type:'menu'
        }
    ],
    admin: [
        {
            name:'image',
            strokeWidth:this.dftStrWidth,
            imageSrc:'image.png',
            title:'VM Images',
            x:0,
            y:0,
            path:"panel/",
            type:'panel'
        },{
            name:'buildplan',
            strokeWidth:this.dftStrWidth,
            imageSrc:'manage_application.png',
            title:'Build Plans',
            x:1,
            y:0,
            path:"panel/",
            type:'panel'
        },{
            name:'domain',
            strokeWidth:this.dftStrWidth,
            imageSrc:'Domain.png',
            title:'Domains',
            x:2,
            y:0,
            path:"panel/",
            type:'panel'
        }

    ],
    manage: [
        {
            name:'access',
            strokeWidth:this.dftStrWidth,
            imageSrc:'Gnome-Security-Medium-64.png',
            title:'Access',
            x:0,
            y:0,
            type:'menu'
        },{
            name:'system',
            strokeWidth:this.dftStrWidth,
            imageSrc:'Gnome-Network-Server-64.png',
            title:'System',
            x:1,
            y:0,
            type:'menu'
        },{
            name:'upload',
            strokeWidth:this.dftStrWidth,
            imageSrc:'Gnome-Go-Up-64.png',
            title:'Upload',
            x:2,
            y:0,
            type:'view'
        }

    ],
    access: [
        {
            name:'usr',
            strokeWidth:this.dftStrWidth,
            imageSrc:'usr-64.png',
            title:'Users',
            x:0,
            y:0,
            type:'panel'
        },{
            name:'ugroup',
            strokeWidth:this.dftStrWidth,
            imageSrc:'Groups-64.png',
            useGray:true,
            title:'User Groups',
            x:1,
            y:0,
            type:'panel'
        },
        {
            name:'entity',
            strokeWidth:this.dftStrWidth,
            imageSrc:'Table_gray.png',
            title:'Server Entities',
            x:2,
            y:0,
            type:'panel'
        }
    ],
    system: [
        {
            name:'session',
            strokeWidth:this.dftStrWidth,
            imageSrc:'session.png',
            title:'Sessions',
            x:0,
            y:0,
            type:'panel'

        },
        {
            name:'dbpool',
            strokeWidth:this.dftStrWidth,
            imageSrc:'Database.png',
            title:'DB Connections',
            x:1,
            y:0,
            type:'panel'

        },{
            name:'logger',
            strokeWidth:this.dftStrWidth,
            imageSrc:'Gnome-Logviewer-64.png',
            title:'System Logs',
            x:2,
            y:0,
            type:'panel'
        },{
            name:'configuration',
            strokeWidth:this.dftStrWidth,
            imageSrc:'Gnome-Preferences-Desktop-64.png',
            title:'Server Config',
            x:3,
            y:0,
            type:'panel'
        },{
            name:'audit',
            strokeWidth:this.dftStrWidth,
            imageSrc:'Gnome-Applications-Office-64.png',
            title:'Request Log',
            x:0,
            y:1,
            type:'panel'
        },{
            name:'storage',
            strokeWidth:this.dftStrWidth,
            imageSrc:'storage.png',
            title:'Storage Locations',
            x:1,
            y:1,
            type:'panel'
        }
        ,{
            name:'encryption',
            strokeWidth:this.dftStrWidth,
            imageSrc:'encryption.png',
            title:'Encryption',
            x:2,
            y:1,
            type:'panel'
        }
    ]
};
