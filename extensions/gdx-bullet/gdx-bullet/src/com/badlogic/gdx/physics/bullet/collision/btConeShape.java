package com.badlogic.gdx.physics.bullet.collision;

/** @author xpenatan */
public class btConeShape extends btConvexInternalShape {

	/*JNI
		#include <src/bullet/BulletCollision/CollisionShapes/btConeShape.h>
	*/
	
	public btConeShape(float radius, float height) {
		resetObj(createNative(radius, height), true);
	}
	
	public static native long createNative(float radius, float height); /*
		return (jlong)new btConeShape(radius, height);
	*/
	/*[0;X;L]
		return Bullet.getPointer(new Bullet.btConeShape(radius, height));
	*/
	
	/*[0;X;F;L]
		protected void cacheObj() {
			addr, this.cPointer #P
			this.jsObj = Bullet.wrapPointer(addr, Bullet.btConeShape); #EVAL
		}
	*/
	
	@Override
	protected void delete() {
		deletePointer(cPointer);
	}
	/*[0;X;D]*/
	
	private static native void deletePointer(long addr); /*
		btConeShape * cobj = (btConeShape *)addr;
		delete cobj;
	*/
	/*[0;X;D]*/
}
